package br.com.erudio.testsupport

import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.MimeMessage

final class MailContent {

    private final List<String> htmlBodies = []
    private final Map<String, byte[]> attachments = [:]

    private MailContent() {}

    static MailContent of(MimeMessage message) {
        def content = new MailContent()
        content.collect(message)
        content
    }

    String html() {
        htmlBodies.join('\n')
    }

    Map<String, byte[]> attachments() {
        attachments
    }

    private void collect(Part part) {
        if (part.isMimeType('multipart/*')) {
            Multipart multipart = part.content as Multipart
            multipart.count.times { int index -> collect(multipart.getBodyPart(index)) }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.disposition)) {
            part.inputStream.withCloseable { InputStream content ->
                attachments[part.fileName] = content.readAllBytes()
            }
        } else if (part.isMimeType('text/html')) {
            htmlBodies << (part.content as String)
        }
    }
}
