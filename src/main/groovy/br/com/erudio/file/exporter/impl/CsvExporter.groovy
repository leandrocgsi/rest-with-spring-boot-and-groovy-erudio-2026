package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

import java.nio.charset.StandardCharsets

@Component
class CsvExporter implements PersonExporter {

    @Override
    Resource exportPeople(List<PersonDTO> people) {
        def outputStream = new ByteArrayOutputStream()
        def writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

        CSVFormat csvFormat = CSVFormat.Builder.create()
            .setHeader('ID', 'First Name', 'Last Name', 'Address', 'Gender', 'Enabled')
            .setSkipHeaderRecord(false)
            .get()

        new CSVPrinter(writer, csvFormat).withCloseable { CSVPrinter csvPrinter ->
            people.each { PersonDTO person ->
                csvPrinter.printRecord(
                    person.id,
                    person.firstName,
                    person.lastName,
                    person.address,
                    person.gender,
                    person.enabled
                )
            }
        }
        new ByteArrayResource(outputStream.toByteArray())
    }

    @Override
    Resource exportPerson(PersonDTO person) {
        null
    }
}
