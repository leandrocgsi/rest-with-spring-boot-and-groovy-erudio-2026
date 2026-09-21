package br.com.erudio.file.importer.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.importer.contract.FileImporter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.stereotype.Component

@Component
class CsvImporter implements FileImporter {

    @Override
    List<PersonDTO> importFile(InputStream inputStream) {
        CSVFormat format = CSVFormat.Builder.create()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get()

        format.parse(new InputStreamReader(inputStream)).withCloseable { CSVParser parser ->
            parser.collect { CSVRecord record ->
                new PersonDTO(
                    firstName: record.get('first_name'),
                    lastName: record.get('last_name'),
                    address: record.get('address'),
                    gender: record.get('gender'),
                    enabled: true
                )
            }
        }
    }
}
