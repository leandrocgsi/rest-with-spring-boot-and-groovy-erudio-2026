package br.com.erudio.unittests.services

import br.com.erudio.config.EmailConfig
import br.com.erudio.config.EmailDefaultsConfig
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.mail.EmailSender
import br.com.erudio.services.EmailService
import br.com.erudio.testsupport.ReturnsSelf
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static java.nio.charset.StandardCharsets.UTF_8

class EmailServiceSpec extends Specification {

    static final String DEFAULT_SUBJECT = 'Default Subject'
    static final String DEFAULT_MESSAGE = 'Default Message'

    EmailSender emailSender = Mock(defaultResponse: new ReturnsSelf())
    EmailConfig emailConfig = new EmailConfig()
    EmailDefaultsConfig defaults = new EmailDefaultsConfig(subject: DEFAULT_SUBJECT, message: DEFAULT_MESSAGE)

    EmailService service = new EmailService(emailSender, emailConfig, defaults)

    def 'sendSimpleEmail uses the subject and body of the request'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', 'Welcome', '<p>Hello Ada</p>'))

        then:
        1 * emailSender.to('ada@erudio.test')
        1 * emailSender.withSubject('Welcome')
        1 * emailSender.withMessage('<p>Hello Ada</p>')
        1 * emailSender.send(emailConfig)
        0 * emailSender.attach(*_)
    }

    def 'sendSimpleEmail sends the body as the message, not the subject'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', 'Just the subject', 'The real body'))

        then:
        1 * emailSender.withMessage('The real body')
        0 * emailSender.withMessage('Just the subject')
    }

    def 'sendSimpleEmail keeps any subject and body the caller sets'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', "$DEFAULT_SUBJECT (custom)", "$DEFAULT_MESSAGE (custom)"))

        then:
        1 * emailSender.withSubject("$DEFAULT_SUBJECT (custom)")
        1 * emailSender.withMessage("$DEFAULT_MESSAGE (custom)")
    }

    def 'sendSimpleEmail falls back to the defaults only when nothing was informed'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', null, null))

        then:
        1 * emailSender.withSubject(DEFAULT_SUBJECT)
        1 * emailSender.withMessage(DEFAULT_MESSAGE)
        1 * emailSender.send(emailConfig)
    }

    def 'sendSimpleEmail treats blank values as missing'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', '   ', ''))

        then:
        1 * emailSender.withSubject(DEFAULT_SUBJECT)
        1 * emailSender.withMessage(DEFAULT_MESSAGE)
    }

    def 'sendSimpleEmail defaults each field independently (#subject / #body)'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', subject, body))

        then:
        1 * emailSender.withSubject(expectedSubject)
        1 * emailSender.withMessage(expectedBody)

        where:
        subject            | body            || expectedSubject   | expectedBody
        'Only the subject' | null            || 'Only the subject' | DEFAULT_MESSAGE
        null               | 'Only the body' || DEFAULT_SUBJECT    | 'Only the body'
    }

    def 'sendEmailWithAttachment uses the values of the request and attaches the file'() {
        given:
        Path attachedPath = null
        String attachedContent = null
        String attachedName = null

        when:
        service.setEmailWithAttachment(
            '{"to":"ada@erudio.test","subject":"Report","body":"See the file"}',
            attachment('report.txt', 'the report'))

        then:
        1 * emailSender.to('ada@erudio.test')
        1 * emailSender.withSubject('Report')
        1 * emailSender.withMessage('See the file')
        1 * emailSender.attach(_ as String, _ as String) >> { String path, String name ->
            attachedPath = Path.of(path)
            attachedName = name
            attachedContent = Files.readString(attachedPath)
            emailSender
        }
        1 * emailSender.send(emailConfig)

        attachedContent == 'the report'
        attachedName == 'report.txt'
        attachedPath.fileName.toString() != 'report.txt'
        !Files.exists(attachedPath)
    }

    def 'sendEmailWithAttachment falls back to the defaults when the request has no subject or body'() {
        when:
        service.setEmailWithAttachment('{"to":"ada@erudio.test"}', attachment('a.txt', 'x'))

        then:
        1 * emailSender.withSubject(DEFAULT_SUBJECT)
        1 * emailSender.withMessage(DEFAULT_MESSAGE)
        1 * emailSender.send(emailConfig)
    }

    def 'sendEmailWithAttachment rejects #description'() {
        when:
        service.setEmailWithAttachment(json, attachment('a.txt', 'x'))

        then:
        def e = thrown(RuntimeException)
        e.message == 'Error parsing email request JSON!'
        0 * emailSender._

        where:
        description                | json
        'an invalid JSON'          | '{not json'
        'unknown fields in the JSON' | '{"to":"ada@erudio.test","cc":"bob@erudio.test"}'
    }

    def 'sendEmailWithAttachment reports a failure reading the attachment'() {
        given:
        MultipartFile broken = Mock {
            getOriginalFilename() >> 'broken.txt'
            transferTo(_ as File) >> { throw new IOException('disk full') }
        }

        when:
        service.setEmailWithAttachment('{"to":"ada@erudio.test"}', broken)

        then:
        def e = thrown(RuntimeException)
        e.message == 'Error processing the attachment!'
        e.cause instanceof IOException
        0 * emailSender.send(_)
    }

    def 'sendEmailWithAttachment deletes the temporary file even when the send fails'() {
        given:
        Path attachedPath = null

        when:
        service.setEmailWithAttachment('{"to":"ada@erudio.test"}', attachment('a.txt', 'x'))

        then:
        1 * emailSender.attach(_ as String, _ as String) >> { String path, String name ->
            attachedPath = Path.of(path)
            emailSender
        }
        1 * emailSender.send(_) >> { throw new IllegalStateException('smtp down') }
        def e = thrown(IllegalStateException)
        e.message == 'smtp down'
        attachedPath != null
        !Files.exists(attachedPath)
    }

    private static EmailRequestDTO request(String to, String subject, String body) {
        new EmailRequestDTO(to: to, subject: subject, body: body)
    }

    private static MultipartFile attachment(String name, String content) {
        new MockMultipartFile('attachment', name, 'text/plain', content.getBytes(UTF_8))
    }
}
