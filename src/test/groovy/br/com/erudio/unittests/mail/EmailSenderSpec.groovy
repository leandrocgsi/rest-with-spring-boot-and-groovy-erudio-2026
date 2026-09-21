package br.com.erudio.unittests.mail

import br.com.erudio.config.EmailConfig
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

import static java.nio.charset.StandardCharsets.UTF_8

class EmailSenderSpec extends Specification {

    static final String SENDER = 'sender@erudio.test'

    @TempDir
    Path tempDir

    JavaMailSender mailSender = Mock()
    EmailConfig config = new EmailConfig(username: SENDER)
    EmailSender sender = new EmailSender(mailSender)
    List<MimeMessage> sent = []

    def setup() {
        def session = Session.getInstance(new Properties())
        mailSender.createMimeMessage() >> { new MimeMessage(session) }
        mailSender.send(_ as MimeMessage) >> { MimeMessage message -> sent << message }
    }

    def 'sends an HTML message from the configured account'() {
        when:
        sender.to('ada@erudio.test').withSubject('Welcome').withMessage('<h1>Hello Ada</h1>').send(config)

        then:
        def message = sentMessage()
        message.subject == 'Welcome'
        addressesOf(message.from) == [SENDER]
        addressesOf(message.getRecipients(Message.RecipientType.TO)) == ['ada@erudio.test']
        MailContent.of(message).html() == '<h1>Hello Ada</h1>'
    }

    def 'sends to several recipients separated by semicolon ignoring spaces'() {
        when:
        sender.to('ada@erudio.test; bob@erudio.test ;carol@erudio.test')
            .withSubject('Team')
            .withMessage('Hi all')
            .send(config)

        then:
        addressesOf(sentMessage().getRecipients(Message.RecipientType.TO)) ==
            ['ada@erudio.test', 'bob@erudio.test', 'carol@erudio.test']
    }

    def 'attaches the given file'() {
        given:
        def file = Files.writeString(tempDir.resolve('report.csv'), 'id,name\n1,Ada\n', UTF_8)

        when:
        sender.to('ada@erudio.test').withSubject('Report').withMessage('See attachment')
            .attach(file.toString())
            .send(config)

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
        sender.to('ada@erudio.test').withSubject('Report').withMessage('See attachment')
            .attach(temporary.toString(), 'Monthly report.csv')
            .send(config)

        then:
        def content = MailContent.of(sentMessage())
        content.attachments().keySet().toList() == ['Monthly report.csv']
        new String(content.attachments()['Monthly report.csv'], UTF_8) == 'id\n1\n'
    }

    def 'a blank attachment name falls back to the name of the file'() {
        given:
        def file = Files.writeString(tempDir.resolve('data.txt'), 'x', UTF_8)

        when:
        sender.to('ada@erudio.test').withSubject('Data').withMessage('See attachment')
            .attach(file.toString(), '  ')
            .send(config)

        then:
        MailContent.of(sentMessage()).attachments().keySet().toList() == ['data.txt']
    }

    def 'sends no attachment when none was given'() {
        when:
        sender.to('ada@erudio.test').withSubject('Plain').withMessage('No files').send(config)

        then:
        MailContent.of(sentMessage()).attachments().isEmpty()
    }

    def 'keeps the accents of the subject and the body'() {
        when:
        sender.to('ada@erudio.test').withSubject('Formação Spring Boot').withMessage('<p>Olá, João!</p>').send(config)

        then:
        def message = sentMessage()
        message.subject == 'Formação Spring Boot'
        MailContent.of(message).html() == '<p>Olá, João!</p>'
    }

    def 'can be reused for another message after a send'() {
        when:
        sender.to('ada@erudio.test').withSubject('First').withMessage('1').send(config)
        sender.to('bob@erudio.test').withSubject('Second').withMessage('2').send(config)

        then:
        sent*.subject == ['First', 'Second']
    }

    def 'rejects an invalid recipient address'() {
        when:
        sender.to('not-an-address@@')

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
        new EmailSender(failing).to('ada@erudio.test').withSubject('Down').withMessage('x').send(config)

        then:
        def e = thrown(MailSendException)
        e.message == 'connection refused'
    }

    private MimeMessage sentMessage() {
        assert sent.size() == 1
        def message = sent.first()
        message.saveChanges()
        message
    }

    private static List<String> addressesOf(def addresses) {
        addresses*.toString()
    }
}
