package br.com.erudio.services

import br.com.erudio.config.EmailConfig
import br.com.erudio.config.EmailDefaultsConfig
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.mail.EmailSender
import groovy.transform.TupleConstructor
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
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
    private final EmailConfig emailConfigs
    private final EmailDefaultsConfig emailDefaults

    void sendSimpleEmail(EmailRequestDTO emailRequest) {
        emailSender
            .to(emailRequest.to)
            .withSubject(subjectOf(emailRequest))
            .withMessage(messageOf(emailRequest))
            .send(emailConfigs)
    }

    void setEmailWithAttachment(String emailRequestJson, MultipartFile attachment) {
        File tempFile = null
        try {
            EmailRequestDTO emailRequest = STRICT_MAPPER.readValue(emailRequestJson, EmailRequestDTO)
            tempFile = File.createTempFile('attachment', attachment.originalFilename)
            attachment.transferTo(tempFile)

            emailSender
                .to(emailRequest.to)
                .withSubject(subjectOf(emailRequest))
                .withMessage(messageOf(emailRequest))
                .attach(tempFile.absolutePath, attachment.originalFilename)
                .send(emailConfigs)
        } catch (JacksonException e) {
            throw new RuntimeException('Error parsing email request JSON!', e)
        } catch (IOException e) {
            throw new RuntimeException('Error processing the attachment!', e)
        } finally {
            if (tempFile?.exists()) tempFile.delete()
        }
    }

    private String subjectOf(EmailRequestDTO emailRequest) {
        StringUtils.hasText(emailRequest.subject) ? emailRequest.subject : emailDefaults.subject
    }

    private String messageOf(EmailRequestDTO emailRequest) {
        StringUtils.hasText(emailRequest.body) ? emailRequest.body : emailDefaults.message
    }
}
