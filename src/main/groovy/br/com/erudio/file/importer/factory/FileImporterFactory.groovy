package br.com.erudio.file.importer.factory

import br.com.erudio.exception.BadRequestException
import br.com.erudio.file.importer.contract.FileImporter
import br.com.erudio.file.importer.impl.CsvImporter
import br.com.erudio.file.importer.impl.XlsxImporter
import groovy.transform.TupleConstructor
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
@TupleConstructor(includeFields = true, defaults = false)
class FileImporterFactory {

    private static final Map<String, Class<? extends FileImporter>> IMPORTERS = [
        '.xlsx': XlsxImporter,
        '.csv' : CsvImporter
    ].asImmutable()

    private final ApplicationContext context

    FileImporter getImporter(String fileName) {
        Class<? extends FileImporter> importer = IMPORTERS.find { fileName.endsWith(it.key) }?.value

        if (importer == null) throw new BadRequestException('Invalid File Format!')
        context.getBean(importer)
    }
}
