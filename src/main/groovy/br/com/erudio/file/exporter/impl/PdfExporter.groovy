package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.services.QRCodeService
import groovy.transform.TupleConstructor
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperPrint
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

import java.util.concurrent.ConcurrentHashMap

@Component
@TupleConstructor(includeFields = true, defaults = false, includes = 'service')
class PdfExporter implements PersonExporter {

    private final QRCodeService service
    private final Map<String, JasperReport> reports = new ConcurrentHashMap<>()

    @Override
    Resource exportPeople(List<PersonDTO> people) {
        Map<String, Object> parameters = [:]

        toPdf(JasperFillManager.fillReport(report('people.jrxml'), parameters, new JRBeanCollectionDataSource(people)))
    }

    @Override
    Resource exportPerson(PersonDTO person) {
        Map<String, Object> parameters = [
            SUB_REPORT_DATA_SOURCE: new JRBeanCollectionDataSource(person.books),
            BOOK_SUB_REPORT       : report('books.jrxml'),
            QR_CODEIMAGE          : service.generateQRCode(person.profileUrl, 200, 200)
        ]

        toPdf(JasperFillManager.fillReport(report('person.jrxml'), parameters, new JRBeanCollectionDataSource([person])))
    }

    private JasperReport report(String template) {
        reports.computeIfAbsent(template) { compile(it) }
    }

    private static JasperReport compile(String template) {
        String path = "/templates/$template"
        InputStream stream = PdfExporter.getResourceAsStream(path)
        if (stream == null) throw new RuntimeException("Template file not found: $path")

        stream.withCloseable { JasperCompileManager.compileReport(it) }
    }

    private static Resource toPdf(JasperPrint jasperPrint) {
        new ByteArrayOutputStream().withCloseable { outputStream ->
            JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream)
            new ByteArrayResource(outputStream.toByteArray())
        }
    }
}
