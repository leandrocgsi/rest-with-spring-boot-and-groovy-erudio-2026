package br.com.erudio.unittests.services

import br.com.erudio.config.EmailDefaultsConfig
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.mail.EmailMessage
import br.com.erudio.mail.EmailSender
import br.com.erudio.services.EmailService
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

import static java.nio.charset.StandardCharsets.UTF_8

class EmailServiceSpec extends Specification {

    static final String DEFAULT_SUBJECT = 'Default Subject'
    static final String DEFAULT_MESSAGE = 'Default Message'

    EmailSender emailSender = Mock()
    EmailDefaultsConfig defaults = new EmailDefaultsConfig(subject: DEFAULT_SUBJECT, message: DEFAULT_MESSAGE)

    EmailService service = new EmailService(emailSender, defaults)

    def 'sendSimpleEmail uses the recipient, subject and body of the request'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', 'Welcome', '<p>Hello Ada</p>'))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.to == 'ada@erudio.test' &&
                message.subject == 'Welcome' &&
                message.body == '<p>Hello Ada</p>' &&
                message.attachment == null &&
                message.attachmentName == null
        })
    }

    def 'sendSimpleEmail sends the body as the message, not the subject'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', 'Just the subject', 'The real body'))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.body == 'The real body' && message.subject == 'Just the subject'
        })
    }

    def 'sendSimpleEmail keeps any subject and body the caller sets'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', "$DEFAULT_SUBJECT (custom)", "$DEFAULT_MESSAGE (custom)"))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.subject == "$DEFAULT_SUBJECT (custom)" && message.body == "$DEFAULT_MESSAGE (custom)"
        })
    }

    def 'sendSimpleEmail falls back to the defaults only when nothing was informed'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', null, null))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.subject == DEFAULT_SUBJECT && message.body == DEFAULT_MESSAGE
        })
    }

    def 'sendSimpleEmail treats blank values as missing'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', '   ', ''))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.subject == DEFAULT_SUBJECT && message.body == DEFAULT_MESSAGE
        })
    }

    def 'sendSimpleEmail defaults each field independently (#subject / #body)'() {
        when:
        service.sendSimpleEmail(request('ada@erudio.test', subject, body))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.subject == expectedSubject && message.body == expectedBody
        })

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
        service.sendEmailWithAttachment(
            '{"to":"ada@erudio.test","subject":"Report","body":"See the file"}',
            attachment('report.txt', 'the report'))

        then:
        1 * emailSender.send(_ as EmailMessage) >> { EmailMessage message ->
            assert message.to == 'ada@erudio.test'
            assert message.subject == 'Report'
            assert message.body == 'See the file'
            attachedPath = message.attachment.toPath()
            attachedName = message.attachmentName
            attachedContent = Files.readString(attachedPath)
        }

        attachedContent == 'the report'
        attachedName == 'report.txt'
        attachedPath.fileName.toString() != 'report.txt'
        !Files.exists(attachedPath)
    }

    def 'sendEmailWithAttachment falls back to the defaults when the request has no subject or body'() {
        when:
        service.sendEmailWithAttachment('{"to":"ada@erudio.test"}', attachment('a.txt', 'x'))

        then:
        1 * emailSender.send({ EmailMessage message ->
            message.subject == DEFAULT_SUBJECT && message.body == DEFAULT_MESSAGE
        })
    }

    def 'sendEmailWithAttachment rejects #description'() {
        when:
        service.sendEmailWithAttachment(json, attachment('a.txt', 'x'))

        then:
        def e = thrown(RuntimeException)
        e.message == 'Error parsing email request JSON!'
        0 * emailSender._

        where:
        description                  | json
        'an invalid JSON'            | '{not json'
        'unknown fields in the JSON' | '{"to":"ada@erudio.test","cc":"bob@erudio.test"}'
    }

    def 'sendEmailWithAttachment reports a failure reading the attachment'() {
        given:
        MultipartFile broken = Mock {
            getOriginalFilename() >> 'broken.txt'
            transferTo(_ as File) >> { throw new IOException('disk full') }
        }

        when:
        service.sendEmailWithAttachment('{"to":"ada@erudio.test"}', broken)

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
        service.sendEmailWithAttachment('{"to":"ada@erudio.test"}', attachment('a.txt', 'x'))

        then:
        1 * emailSender.send(_ as EmailMessage) >> { EmailMessage message ->
            attachedPath = message.attachment.toPath()
            throw new IllegalStateException('smtp down')
        }
        def e = thrown(IllegalStateException)
        e.message == 'smtp down'
        attachedPath != null
        !Files.exists(attachedPath)
    }

    private static EmailRequestDTO request(String to, String subject, String body) {
        new EmailRequestDTO(to, subject, body)
    }

    private static MultipartFile attachment(String name, String content) {
        new MockMultipartFile('attachment', name, 'text/plain', content.getBytes(UTF_8))
    }
}
