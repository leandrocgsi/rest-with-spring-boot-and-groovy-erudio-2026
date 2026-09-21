package br.com.erudio.unittests.file

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.impl.XlsxExporter
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.core.io.Resource
import spock.lang.Specification

class XlsxExporterSpec extends Specification {

    static final List<String> HEADER = ['ID', 'First Name', 'Last Name', 'Address', 'Gender', 'Enabled']

    XlsxExporter exporter = new XlsxExporter()
    DataFormatter formatter = new DataFormatter()

    def 'writes a sheet named People with the header and one row per person'() {
        when:
        def resource = exporter.exportPeople([
            person(1L, 'Ayrton', 'Senna', 'São Paulo - Brasil', 'Male', true),
            person(2L, 'Marie', 'Curie', 'Warsaw - Poland', 'Female', false)
        ])

        then:
        withWorkbook(resource) { workbook ->
            assert workbook.numberOfSheets == 1
            def sheet = workbook.getSheet('People')
            assert sheet != null

            assert sheet.lastRowNum == 2
            assert rowValues(sheet.getRow(0)) == HEADER
            assert rowValues(sheet.getRow(1)) == ['1', 'Ayrton', 'Senna', 'São Paulo - Brasil', 'Male', 'Yes']
            assert rowValues(sheet.getRow(2)) == ['2', 'Marie', 'Curie', 'Warsaw - Poland', 'Female', 'No']
        }
    }

    def 'writes the id as a number'() {
        when:
        def resource = exporter.exportPeople([person(42L, 'Ada', 'Lovelace', 'London', 'Female', true)])

        then:
        withWorkbook(resource) { workbook ->
            def id = workbook.getSheet('People').getRow(1).getCell(0)

            assert id.cellType == CellType.NUMERIC
            assert id.numericCellValue == 42.0d
        }
    }

    def 'writes No when the enabled flag is missing'() {
        when:
        def resource = exporter.exportPeople([person(3L, 'Alan', 'Turing', 'Wilmslow', 'Male', null)])

        then:
        withWorkbook(resource) { workbook ->
            assert formatter.formatCellValue(workbook.getSheet('People').getRow(1).getCell(5)) == 'No'
        }
    }

    def 'writes the header in bold'() {
        when:
        def resource = exporter.exportPeople([])

        then:
        withWorkbook(resource) { workbook ->
            def header = workbook.getSheet('People').getRow(0).getCell(0)

            assert workbook.getFontAt(header.cellStyle.fontIndex).bold
        }
    }

    def 'writes only the header when there is nobody'() {
        when:
        def resource = exporter.exportPeople([])

        then:
        withWorkbook(resource) { workbook ->
            def sheet = workbook.getSheet('People')

            assert sheet.lastRowNum == 0
            assert rowValues(sheet.getRow(0)) == HEADER
        }
    }

    def 'exporting a single person is not supported'() {
        expect:
        exporter.exportPerson(person(1L, 'Ayrton', 'Senna', 'x', 'Male', true)) == null
    }

    private List<String> rowValues(Row row) {
        HEADER.indices.collect { int index ->
            def cell = row.getCell(index)
            cell == null ? null : formatter.formatCellValue(cell)
        }
    }

    private static void withWorkbook(Resource resource, Closure verification) {
        resource.inputStream.withCloseable { InputStream input ->
            WorkbookFactory.create(input).withCloseable { workbook -> verification(workbook) }
        }
    }

    private static PersonDTO person(Long id, String firstName, String lastName, String address, String gender, Boolean enabled) {
        new PersonDTO(id: id, firstName: firstName, lastName: lastName, address: address, gender: gender, enabled: enabled)
    }
}
