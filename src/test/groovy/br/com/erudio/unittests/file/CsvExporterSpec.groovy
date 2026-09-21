package br.com.erudio.unittests.file

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.impl.CsvExporter
import org.apache.commons.csv.CSVFormat
import org.springframework.core.io.Resource
import spock.lang.Specification

import static java.nio.charset.StandardCharsets.UTF_8

class CsvExporterSpec extends Specification {

    static final List<String> HEADER = ['ID', 'First Name', 'Last Name', 'Address', 'Gender', 'Enabled']

    CsvExporter exporter = new CsvExporter()

    def 'writes the header and one line per person'() {
        when:
        def csv = parse(exporter.exportPeople([
            person(1L, 'Ayrton', 'Senna', 'São Paulo - Brasil', 'Male', true),
            person(2L, 'Leonardo', 'da Vinci', 'Vinci - Italy', 'Male', false)
        ]))

        then:
        csv.header == HEADER
        csv.records.size() == 2
        csv.records[0].toList() == ['1', 'Ayrton', 'Senna', 'São Paulo - Brasil', 'Male', 'true']
        csv.records[1].toList() == ['2', 'Leonardo', 'da Vinci', 'Vinci - Italy', 'Male', 'false']
    }

    def 'writes only the header when there is nobody'() {
        when:
        def csv = parse(exporter.exportPeople([]))

        then:
        csv.header == HEADER
        csv.records.empty
    }

    def 'keeps commas and quotes inside a value'() {
        given:
        def address = 'Rua "A", 10 - apto 2'

        when:
        def csv = parse(exporter.exportPeople([person(7L, 'Ada', 'Lovelace', address, 'Female', true)]))

        then:
        csv.records.size() == 1
        csv.records[0].get('Address') == address
        csv.records[0].size() == 6
    }

    def 'encodes the file as UTF-8'() {
        when:
        def resource = exporter.exportPeople([person(1L, 'João', 'Conceição', 'Avenida São João', 'Male', true)])

        then:
        new String(resource.contentAsByteArray, UTF_8).contains('João,Conceição,Avenida São João')
    }

    def 'writes missing values as empty fields'() {
        when:
        def csv = parse(exporter.exportPeople([person(3L, 'Alan', 'Turing', null, null, null)]))

        then:
        with(csv.records[0]) {
            get('Address') == ''
            get('Gender') == ''
            get('Enabled') == ''
        }
    }

    def 'exporting a single person is not supported'() {
        expect:
        exporter.exportPerson(person(1L, 'Ayrton', 'Senna', 'x', 'Male', true)) == null
    }

    private static PersonDTO person(Long id, String firstName, String lastName, String address, String gender, Boolean enabled) {
        new PersonDTO(id: id, firstName: firstName, lastName: lastName, address: address, gender: gender, enabled: enabled)
    }

    private static Map parse(Resource resource) {
        new InputStreamReader(resource.inputStream, UTF_8).withCloseable { reader ->
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader).withCloseable { parser ->
                [header: parser.headerNames, records: parser.records]
            }
        }
    }
}
