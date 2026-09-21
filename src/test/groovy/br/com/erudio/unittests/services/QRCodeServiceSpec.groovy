package br.com.erudio.unittests.services

import br.com.erudio.services.QRCodeService
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import spock.lang.Specification

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class QRCodeServiceSpec extends Specification {

    QRCodeService service = new QRCodeService()

    def 'generates a PNG with the requested size'() {
        when:
        def image = service.generateQRCode('https://pub.erudio.com.br', 200, 200).withCloseable { ImageIO.read(it) }

        then:
        image.width == 200
        image.height == 200
    }

    def 'generates a QR code that decodes back to "#text"'() {
        when:
        def image = service.generateQRCode(text, 300, 300).withCloseable { ImageIO.read(it) }

        then:
        decode(image) == text

        where:
        text << ['https://en.wikipedia.org/wiki/Ayrton_Senna', 'Formação Spring Boot 2026']
    }

    def 'rejects empty content'() {
        when:
        service.generateQRCode('', 200, 200)

        then:
        thrown(IllegalArgumentException)
    }

    private static String decode(BufferedImage image) {
        def bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))
        new MultiFormatReader().decode(bitmap).text
    }
}
