package br.com.erudio.mail

import br.com.erudio.config.EmailConfig
import groovy.util.logging.Slf4j
import jakarta.mail.MessagingException
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

@Slf4j
@Component
class EmailSender {

    private final JavaMailSender mailSender
    private String to
    private String subject
    private String body
    private List<InternetAddress> recipients = []
    private File attachment
    private String attachmentName

    EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender
    }

    EmailSender to(String to) {
        this.to = to
        this.recipients = getRecipients(to)
        this
    }

    EmailSender withSubject(String subject) {
        this.subject = subject
        this
    }

    EmailSender withMessage(String body) {
        this.body = body
        this
    }

    EmailSender attach(String fileDir, String attachmentName = null) {
        this.attachment = new File(fileDir)
        this.attachmentName = StringUtils.hasText(attachmentName) ? attachmentName : attachment.name
        this
    }

    void send(EmailConfig config) {
        MimeMessage message = mailSender.createMimeMessage()
        try {
            def helper = new MimeMessageHelper(message, true)
            helper.setFrom(config.username)
            helper.setTo(recipients as InternetAddress[])
            helper.setSubject(subject)
            helper.setText(body, true)
            if (attachment != null) {
                helper.addAttachment(attachmentName, attachment)
            }
            mailSender.send(message)
            log.info("Email sent to $to with the subject '$subject'")
            reset()
        } catch (MessagingException e) {
            throw new RuntimeException('Error sending the email', e)
        }
    }

    private void reset() {
        to = null
        subject = null
        body = null
        recipients = null
        attachment = null
        attachmentName = null
    }

    private static List<InternetAddress> getRecipients(String to) {
        to.replaceAll(/\s/, '').tokenize(';').collect { String address ->
            try {
                new InternetAddress(address)
            } catch (AddressException e) {
                throw new RuntimeException(e)
            }
        }
    }
}
