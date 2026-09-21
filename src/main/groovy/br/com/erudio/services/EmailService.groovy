package br.com.erudio.services

import br.com.erudio.config.EmailDefaultsConfig
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.mail.EmailMessage
import br.com.erudio.mail.EmailSender
import groovy.transform.TupleConstructor
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

@Service
@TupleConstructor(includeFields = true, defaults = false)
class EmailService {

    private static final JsonMapper STRICT_MAPPER = JsonMapper.builder()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    private final EmailSender emailSender
    private final EmailDefaultsConfig emailDefaults

    void sendSimpleEmail(EmailRequestDTO emailRequest) {
        emailSender.send(messageFor(emailRequest))
    }

    void sendEmailWithAttachment(String emailRequestJson, MultipartFile attachment) {
        File tempFile = null
        try {
            EmailRequestDTO emailRequest = STRICT_MAPPER.readValue(emailRequestJson, EmailRequestDTO)
            tempFile = File.createTempFile('attachment', attachment.originalFilename)
            attachment.transferTo(tempFile)

            emailSender.send(messageFor(emailRequest, tempFile, attachment.originalFilename))
        } catch (JacksonException e) {
            throw new RuntimeException('Error parsing email request JSON!', e)
        } catch (IOException e) {
            throw new RuntimeException('Error processing the attachment!', e)
        } finally {
            if (tempFile?.exists()) tempFile.delete()
        }
    }

    private EmailMessage messageFor(EmailRequestDTO emailRequest, File attachment = null, String attachmentName = null) {
        new EmailMessage(
            to: emailRequest.to,
            subject: emailRequest.subject?.trim() ? emailRequest.subject : emailDefaults.subject,
            body: emailRequest.body?.trim() ? emailRequest.body : emailDefaults.message,
            attachment: attachment,
            attachmentName: attachmentName
        )
    }
}
