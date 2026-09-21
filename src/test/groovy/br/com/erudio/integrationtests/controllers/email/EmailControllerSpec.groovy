package br.com.erudio.integrationtests.controllers.email

import br.com.erudio.integrationtests.AuthenticatedIntegrationSpec
import br.com.erudio.testsupport.MailContent
import jakarta.mail.Message
import jakarta.mail.internet.MimeMessage
import io.restassured.response.ValidatableResponse

import static io.restassured.RestAssured.given
import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.Matchers.equalTo

class EmailControllerSpec extends AuthenticatedIntegrationSpec {

    static final String BASE = '/api/email/v1'
    static final String ATTACHMENT_URL = "$BASE/withAttachment"
    static final String SENDER = 'sender@erudio.test'

    static final String DEFAULT_SUBJECT = 'Default Subject'
    static final String DEFAULT_MESSAGE = 'Default Message'

    def setup() {
        greenMail().purgeEmailFromAllMailboxes()
    }

    def 'the subject and the body of the request arrive as they were sent'() {
        when:
        sendSimple([to: 'ada@erudio.test', subject: 'Welcome to the course', body: '<h1>Hello Ada</h1><p>See you in class.</p>'])

        then:
        def message = onlyMessage()
        message.subject == 'Welcome to the course'
        message.from[0].toString() == SENDER
        recipientsOf(message) == ['ada@erudio.test']
        MailContent.of(message).html() == '<h1>Hello Ada</h1><p>See you in class.</p>'
        MailContent.of(message).attachments().isEmpty()
    }

    def 'accents survive the trip through the mail server'() {
        when:
        sendSimple([to: 'joao@erudio.test', subject: 'Formação Spring Boot 2026', body: '<p>Olá, João! Até a próxima aula.</p>'])

        then:
        def message = onlyMessage()
        message.subject == 'Formação Spring Boot 2026'
        MailContent.of(message).html() == '<p>Olá, João! Até a próxima aula.</p>'
    }

    def 'several recipients separated by semicolon all receive the message'() {
        when:
        sendSimple([to: 'ada@erudio.test; bob@erudio.test ;carol@erudio.test', subject: 'Team', body: 'Hi all'])

        then:
        greenMail().waitForIncomingEmail(5000, 3)
        def messages = greenMail().receivedMessages
        messages.length == 3
        messages.every { MimeMessage message ->
            recipientsOf(message) == ['ada@erudio.test', 'bob@erudio.test', 'carol@erudio.test'] && message.subject == 'Team'
        }
    }

    def 'the defaults are used only for the fields the request leaves out (#request)'() {
        when:
        sendSimple(request)

        then:
        def message = onlyMessage()
        message.subject == expectedSubject
        MailContent.of(message).html() == expectedMessage

        where:
        request                                                        || expectedSubject   | expectedMessage
        [to: 'ada@erudio.test']                                        || DEFAULT_SUBJECT   | DEFAULT_MESSAGE
        [to: 'ada@erudio.test', subject: 'My own subject']             || 'My own subject'  | DEFAULT_MESSAGE
        [to: 'ada@erudio.test', body: 'My own body']                   || DEFAULT_SUBJECT   | 'My own body'
        [to: 'ada@erudio.test', subject: '   ', body: '']              || DEFAULT_SUBJECT   | DEFAULT_MESSAGE
    }

    def 'an invalid recipient fails and nothing is delivered'() {
        when:
        given().spec(authenticated())
            .contentType('application/json')
            .body([to: 'not-an-address@@', subject: 'x', body: 'y'])
        .when()
            .post(BASE)
        .then()
            .statusCode(500)

        then:
        greenMail().receivedMessages.length == 0
    }

    def 'a malformed JSON body is a bad request'() {
        when:
        given().spec(authenticated())
            .contentType('application/json')
            .body('{not json')
        .when()
            .post(BASE)
        .then()
            .statusCode(400)

        then:
        greenMail().receivedMessages.length == 0
    }

    def 'the attachment arrives with the subject and body of the request'() {
        given:
        def report = 'id,name\n1,Ada\n2,Alan\n'.getBytes(UTF_8)

        when:
        sendWithAttachment(
            '{"to":"ada@erudio.test","subject":"Monthly report","body":"<p>The report is attached.</p>"}',
            'report.csv', report)
            .statusCode(200)
            .body(equalTo('e-Mail with attachment sent successfully!'))

        then:
        def message = onlyMessage()
        def content = MailContent.of(message)
        message.subject == 'Monthly report'
        recipientsOf(message) == ['ada@erudio.test']
        content.html() == '<p>The report is attached.</p>'
        content.attachments().keySet().toList() == ['report.csv']
        new String(content.attachments()['report.csv'], UTF_8).replace('\r\n', '\n') == new String(report, UTF_8)
    }

    def 'a binary attachment survives byte for byte'() {
        given:
        def binary = new byte[300 * 1024]
        new Random(7).nextBytes(binary)

        when:
        sendWithAttachment('{"to":"ada@erudio.test","subject":"Binary","body":"see file"}', 'data.bin', binary)
            .statusCode(200)

        then:
        MailContent.of(onlyMessage()).attachments()['data.bin'] == binary
    }

    def 'the attachment e-mail falls back to the defaults when the request has no subject or body'() {
        when:
        sendWithAttachment('{"to":"ada@erudio.test"}', 'notes.txt', 'notes'.getBytes(UTF_8))
            .statusCode(200)

        then:
        def message = onlyMessage()
        def content = MailContent.of(message)
        message.subject == DEFAULT_SUBJECT
        content.html() == DEFAULT_MESSAGE
        content.attachments()['notes.txt'] == 'notes'.getBytes(UTF_8)
    }

    def 'the attachment e-mail keeps any subject the caller sets'() {
        when:
        sendWithAttachment('{"to":"ada@erudio.test","subject":"Only my subject"}', 'notes.txt', 'n'.getBytes(UTF_8))
            .statusCode(200)

        then:
        def message = onlyMessage()
        message.subject == 'Only my subject'
        MailContent.of(message).html() == DEFAULT_MESSAGE
    }

    def 'an attachment request with #description is rejected and nothing is delivered'() {
        when:
        sendWithAttachment(requestJson, 'notes.txt', 'n'.getBytes(UTF_8))
            .statusCode(500)
            .body('message', equalTo('Error parsing email request JSON!'))

        then:
        greenMail().receivedMessages.length == 0

        where:
        description                  | requestJson
        'an invalid JSON'            | '{not json'
        'unknown fields in the JSON' | '{"to":"ada@erudio.test","cc":"bob@erudio.test"}'
    }

    def 'an attachment request with an invalid recipient is rejected and nothing is delivered'() {
        when:
        sendWithAttachment('{"to":"not-an-address@@","subject":"x"}', 'notes.txt', 'n'.getBytes(UTF_8))
            .statusCode(500)

        then:
        greenMail().receivedMessages.length == 0
    }

    def 'the attachment part is required'() {
        when:
        given().spec(authenticated())
            .multiPart('emailRequest', '{"to":"ada@erudio.test"}')
        .when()
            .post(ATTACHMENT_URL)
        .then()
            .statusCode(400)

        then:
        greenMail().receivedMessages.length == 0
    }

    def 'the request part is required'() {
        expect:
        given().spec(authenticated())
            .multiPart('attachment', 'notes.txt', 'n'.getBytes(UTF_8), 'text/plain')
        .when()
            .post(ATTACHMENT_URL)
        .then()
            .statusCode(400)
    }

    def 'the attachment endpoint only accepts multipart'() {
        expect:
        given().spec(authenticated())
            .contentType('application/json')
            .body('{"to":"ada@erudio.test"}')
        .when()
            .post(ATTACHMENT_URL)
        .then()
            .statusCode(415)
    }

    def 'both endpoints require authentication'() {
        when:
        given().spec(anonymous())
            .contentType('application/json')
            .body([to: 'ada@erudio.test'])
        .when()
            .post(BASE)
        .then()
            .statusCode(403)

        given().spec(anonymous())
            .multiPart('emailRequest', '{"to":"ada@erudio.test"}')
            .multiPart('attachment', 'notes.txt', 'n'.getBytes(UTF_8), 'text/plain')
        .when()
            .post(ATTACHMENT_URL)
        .then()
            .statusCode(403)

        then:
        greenMail().receivedMessages.length == 0
    }

    private static void sendSimple(Map<String, Object> request) {
        given().spec(authenticated())
            .contentType('application/json')
            .body(request)
        .when()
            .post(BASE)
        .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(equalTo('e-Mail sent with success!'))
    }

    private static ValidatableResponse sendWithAttachment(String requestJson, String attachmentName, byte[] attachment) {
        given().spec(authenticated())
            .multiPart('emailRequest', requestJson)
            .multiPart('attachment', attachmentName, attachment, 'application/octet-stream')
        .when()
            .post(ATTACHMENT_URL)
        .then()
            .log().ifValidationFails()
    }

    private static MimeMessage onlyMessage() {
        assert greenMail().waitForIncomingEmail(5000, 1): 'the e-mail never arrived at the SMTP server'
        def messages = greenMail().receivedMessages
        assert messages.length == 1
        messages[0]
    }

    private static List<String> recipientsOf(MimeMessage message) {
        message.getRecipients(Message.RecipientType.TO)*.toString()
    }
}
