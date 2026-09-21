package br.com.erudio.services

import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.springframework.stereotype.Service

@Service
class QRCodeService {

    InputStream generateQRCode(String url, int width, int height) {
        BitMatrix bitMatrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, width, height)

        def outputStream = new ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, 'PNG', outputStream)
        new ByteArrayInputStream(outputStream.toByteArray())
    }
}
