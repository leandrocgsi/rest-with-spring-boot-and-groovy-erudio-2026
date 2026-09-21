package br.com.erudio.unittests.file

import br.com.erudio.exception.BadRequestException
import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.exporter.impl.CsvExporter
import br.com.erudio.file.exporter.impl.PdfExporter
import br.com.erudio.file.exporter.impl.XlsxExporter
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.file.importer.impl.CsvImporter
import br.com.erudio.file.importer.impl.XlsxImporter
import br.com.erudio.services.QRCodeService
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class FileFactoriesSpec extends Specification {

    ApplicationContext context = Stub()

    def 'the exporter factory picks the exporter from the accept header'() {
        given:
        def xlsx = new XlsxExporter()
        def csv = new CsvExporter()
        def pdf = new PdfExporter(new QRCodeService())
        context.getBean(XlsxExporter) >> xlsx
        context.getBean(CsvExporter) >> csv
        context.getBean(PdfExporter) >> pdf
        def factory = new FileExporterFactory(context)

        expect:
        factory.getExporter(MediaTypes.APPLICATION_XLSX_VALUE).is(xlsx)
        factory.getExporter(MediaTypes.APPLICATION_CSV_VALUE).is(csv)
        factory.getExporter(MediaTypes.APPLICATION_PDF_VALUE).is(pdf)
    }

    def 'the accept header is matched ignoring case'() {
        given:
        def csv = new CsvExporter()
        def pdf = new PdfExporter(new QRCodeService())
        context.getBean(CsvExporter) >> csv
        context.getBean(PdfExporter) >> pdf
        def factory = new FileExporterFactory(context)

        expect:
        factory.getExporter('TEXT/CSV').is(csv)
        factory.getExporter('Application/PDF').is(pdf)
    }

    def 'the exporter factory rejects the format "#accept"'() {
        when:
        new FileExporterFactory(context).getExporter(accept)

        then:
        def e = thrown(BadRequestException)
        e.message == 'Invalid File Format!'

        where:
        accept << ['application/json', 'application/xml', 'text/plain', '*/*', '']
    }

    def 'the exporter it returns is usable'() {
        given:
        context.getBean(CsvExporter) >> new CsvExporter()

        when:
        def exporter = new FileExporterFactory(context).getExporter(MediaTypes.APPLICATION_CSV_VALUE)

        then:
        exporter.exportPeople([]).contentLength() > 0
    }

    def 'the importer factory picks the importer from the file extension'() {
        given:
        def xlsx = new XlsxImporter()
        def csv = new CsvImporter()
        context.getBean(XlsxImporter) >> xlsx
        context.getBean(CsvImporter) >> csv
        def factory = new FileImporterFactory(context)

        expect:
        factory.getImporter('people.xlsx').is(xlsx)
        factory.getImporter('people.csv').is(csv)
    }

    def 'only the last extension counts'() {
        given:
        def xlsx = new XlsxImporter()
        def csv = new CsvImporter()
        context.getBean(XlsxImporter) >> xlsx
        context.getBean(CsvImporter) >> csv
        def factory = new FileImporterFactory(context)

        expect:
        factory.getImporter('people.xlsx.csv').is(csv)
        factory.getImporter('my people (1).xlsx').is(xlsx)
    }

    def 'the importer factory rejects the file "#fileName"'() {
        when:
        new FileImporterFactory(context).getImporter(fileName)

        then:
        def e = thrown(BadRequestException)
        e.message == 'Invalid File Format!'

        where:
        fileName << ['people.txt', 'people.xls', 'people.pdf', 'people', 'csv', 'people.csv.bak', '']
    }
}
