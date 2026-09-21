package br.com.erudio.unittests.mail

import br.com.erudio.config.EmailConfig
import br.com.erudio.mail.EmailMessage
import br.com.erudio.mail.EmailSender
import br.com.erudio.testsupport.MailContent
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.MailSendException
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors

import static java.nio.charset.StandardCharsets.UTF_8

class EmailSenderSpec extends Specification {

    static final String SENDER = 'sender@erudio.test'

    @TempDir
    Path tempDir

    JavaMailSender mailSender = Mock()
    EmailConfig config = new EmailConfig(username: SENDER)
    EmailSender sender = new EmailSender(mailSender, config)
    List<MimeMessage> sent = Collections.synchronizedList([])

    def setup() {
        def session = Session.getInstance(new Properties())
        mailSender.createMimeMessage() >> { new MimeMessage(session) }
        mailSender.send(_ as MimeMessage) >> { MimeMessage message -> sent << message }
    }

    def 'sends an HTML message from the configured account'() {
        when:
        sender.send(new EmailMessage(to: 'ada@erudio.test', subject: 'Welcome', body: '<h1>Hello Ada</h1>'))

        then:
        def message = sentMessage()
        message.subject == 'Welcome'
        addressesOf(message.from) == [SENDER]
        addressesOf(message.getRecipients(Message.RecipientType.TO)) == ['ada@erudio.test']
        MailContent.of(message).html() == '<h1>Hello Ada</h1>'
    }

    def 'sends to several recipients separated by semicolon ignoring spaces'() {
        when:
        sender.send(new EmailMessage(to: 'ada@erudio.test; bob@erudio.test ;carol@erudio.test', subject: 'Team', body: 'Hi all'))

        then:
        addressesOf(sentMessage().getRecipients(Message.RecipientType.TO)) ==
            ['ada@erudio.test', 'bob@erudio.test', 'carol@erudio.test']
    }

    def 'attaches the given file'() {
        given:
        def file = Files.writeString(tempDir.resolve('report.csv'), 'id,name\n1,Ada\n', UTF_8)

        when:
        sender.send(new EmailMessage(to: 'ada@erudio.test', subject: 'Report', body: 'See attachment', attachment: file.toFile()))

        then:
        def content = MailContent.of(sentMessage())
        content.html() == 'See attachment'
        content.attachments().size() == 1
        new String(content.attachments()['report.csv'], UTF_8) == 'id,name\n1,Ada\n'
    }

    def 'the recipient sees the given attachment name instead of the name of the file'() {
        given:
        def temporary = Files.writeString(tempDir.resolve('attachment8291746352report.csv'), 'id\n1\n', UTF_8)

        when:
        sender.send(new EmailMessage(
            to: 'ada@erudio.test', subject: 'Report', body: 'See attachment',
            attachment: temporary.toFile(), attachmentName: 'Monthly report.csv'))

        then:
        def content = MailContent.of(sentMessage())
        content.attachments().keySet().toList() == ['Monthly report.csv']
        new String(content.attachments()['Monthly report.csv'], UTF_8) == 'id\n1\n'
    }

    def 'a blank attachment name falls back to the name of the file'() {
        given:
        def file = Files.writeString(tempDir.resolve('data.txt'), 'x', UTF_8)

        when:
        sender.send(new EmailMessage(
            to: 'ada@erudio.test', subject: 'Data', body: 'See attachment',
            attachment: file.toFile(), attachmentName: '  '))

        then:
        MailContent.of(sentMessage()).attachments().keySet().toList() == ['data.txt']
    }

    def 'sends no attachment when none was given'() {
        when:
        sender.send(new EmailMessage(to: 'ada@erudio.test', subject: 'Plain', body: 'No files'))

        then:
        MailContent.of(sentMessage()).attachments().isEmpty()
    }

    def 'keeps the accents of the subject and the body'() {
        when:
        sender.send(new EmailMessage(to: 'ada@erudio.test', subject: 'Formação Spring Boot', body: '<p>Olá, João!</p>'))

        then:
        def message = sentMessage()
        message.subject == 'Formação Spring Boot'
        MailContent.of(message).html() == '<p>Olá, João!</p>'
    }

    def 'one message does not leak into the next one'() {
        given:
        def file = Files.writeString(tempDir.resolve('first.txt'), 'first', UTF_8)

        when:
        sender.send(new EmailMessage(to: 'ada@erudio.test', subject: 'First', body: '1', attachment: file.toFile()))
        sender.send(new EmailMessage(to: 'bob@erudio.test', subject: 'Second', body: '2'))

        then:
        sent.size() == 2
        def first = saved(sent[0])
        def second = saved(sent[1])
        [first.subject, second.subject] == ['First', 'Second']
        !MailContent.of(first).attachments().isEmpty()
        MailContent.of(second).attachments().isEmpty()
        addressesOf(second.getRecipients(Message.RecipientType.TO)) == ['bob@erudio.test']
    }

    def 'is safe to use from several threads at the same time'() {
        when:
        Executors.newFixedThreadPool(8).withCloseable { pool ->
            (1..40)
                .collect { int index ->
                    pool.submit({
                        sender.send(new EmailMessage(to: "user$index@erudio.test", subject: "Subject $index", body: "Body $index"))
                    } as Callable)
                }
                .each { it.get() }
        }

        then:
        sent.size() == 40
        sent.every { MimeMessage message ->
            saved(message)
            def index = message.subject.substring('Subject '.length())
            addressesOf(message.getRecipients(Message.RecipientType.TO)) == ["user$index@erudio.test".toString()] &&
                MailContent.of(message).html() == "Body $index"
        }
    }

    def 'rejects an invalid recipient address'() {
        when:
        sender.send(new EmailMessage(to: 'not-an-address@@', subject: 'x', body: 'y'))

        then:
        def e = thrown(RuntimeException)
        e.cause instanceof AddressException
        sent.empty
    }

    def 'propagates a failure of the mail server'() {
        given:
        def failing = Mock(JavaMailSender) {
            createMimeMessage() >> new MimeMessage(Session.getInstance(new Properties()))
            send(_ as MimeMessage) >> { throw new MailSendException('connection refused') }
        }

        when:
        new EmailSender(failing, config).send(new EmailMessage(to: 'ada@erudio.test', subject: 'Down', body: 'x'))

        then:
        def e = thrown(MailSendException)
        e.message == 'connection refused'
    }

    private MimeMessage sentMessage() {
        assert sent.size() == 1
        saved(sent.first())
    }

    private static MimeMessage saved(MimeMessage message) {
        message.saveChanges()
        message
    }

    private static List<String> addressesOf(def addresses) {
        addresses*.toString()
    }
}
