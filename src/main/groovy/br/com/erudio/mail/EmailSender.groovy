package br.com.erudio.mail

import br.com.erudio.config.EmailConfig
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import jakarta.mail.MessagingException
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Slf4j
@Component
@TupleConstructor(includeFields = true, defaults = false)
class EmailSender {

    private final JavaMailSender mailSender
    private final EmailConfig config

    void send(EmailMessage message) {
        List<InternetAddress> recipients = recipientsOf(message.to)
        MimeMessage mimeMessage = mailSender.createMimeMessage()

        try {
            def helper = new MimeMessageHelper(mimeMessage, true)
            helper.setFrom(config.username)
            helper.setTo(recipients as InternetAddress[])
            helper.setSubject(message.subject)
            helper.setText(message.body, true)
            if (message.attachment) {
                helper.addAttachment(attachmentNameOf(message), message.attachment)
            }
            mailSender.send(mimeMessage)
            log.info("Email sent to $message.to with the subject '$message.subject'")
        } catch (MessagingException e) {
            throw new RuntimeException('Error sending the email', e)
        }
    }

    private static String attachmentNameOf(EmailMessage message) {
        message.attachmentName?.trim() ? message.attachmentName : message.attachment.name
    }

    private static List<InternetAddress> recipientsOf(String to) {
        to.replaceAll(/\s/, '').tokenize(';').collect { address ->
            try {
                new InternetAddress(address)
            } catch (AddressException e) {
                throw new RuntimeException(e)
            }
        }
    }
}
