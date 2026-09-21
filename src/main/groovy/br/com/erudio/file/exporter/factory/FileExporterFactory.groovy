package br.com.erudio.file.exporter.factory

import br.com.erudio.exception.BadRequestException
import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.file.exporter.impl.CsvExporter
import br.com.erudio.file.exporter.impl.PdfExporter
import br.com.erudio.file.exporter.impl.XlsxExporter
import groovy.transform.TupleConstructor
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
@TupleConstructor(includeFields = true, defaults = false)
class FileExporterFactory {

    private static final Map<String, Class<? extends PersonExporter>> EXPORTERS = [
        (MediaTypes.APPLICATION_XLSX_VALUE): XlsxExporter,
        (MediaTypes.APPLICATION_CSV_VALUE) : CsvExporter,
        (MediaTypes.APPLICATION_PDF_VALUE) : PdfExporter
    ]

    private final ApplicationContext context

    PersonExporter getExporter(String acceptHeader) {
        Class<? extends PersonExporter> exporter = EXPORTERS.find { String mediaType, Class<? extends PersonExporter> type ->
            mediaType.equalsIgnoreCase(acceptHeader)
        }?.value

        if (exporter == null) throw new BadRequestException('Invalid File Format!')
        context.getBean(exporter)
    }
}
