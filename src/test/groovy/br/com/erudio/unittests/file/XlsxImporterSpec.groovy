package br.com.erudio.unittests.file

import br.com.erudio.file.importer.impl.XlsxImporter
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import spock.lang.Specification

class XlsxImporterSpec extends Specification {

    XlsxImporter importer = new XlsxImporter()

    def 'imports one person per row skipping the header'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            row(sheet, 1, 'Ada', 'Lovelace', 'London', 'Female')
            row(sheet, 2, 'Alan', 'Turing', 'Wilmslow', 'Male')
        })

        then:
        people.size() == 2
        with(people[0]) {
            firstName == 'Ada'
            lastName == 'Lovelace'
            address == 'London'
            gender == 'Female'
        }
        people[1].firstName == 'Alan'
    }

    def 'imported people are enabled and have no id yet'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            row(sheet, 1, 'Ada', 'Lovelace', 'London', 'Female')
        })

        then:
        people[0].enabled
        people[0].id == null
    }

    def 'the first row is always the header whatever it contains'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            row(sheet, 0, 'Grace', 'Hopper', 'New York', 'Female')
            row(sheet, 1, 'Ada', 'Lovelace', 'London', 'Female')
        })

        then:
        people.size() == 1
        people[0].firstName == 'Ada'
    }

    def 'skips rows whose first cell is blank'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            row(sheet, 1, 'Ada', 'Lovelace', 'London', 'Female')
            def blank = sheet.createRow(2)
            blank.createCell(0, CellType.BLANK)
            blank.createCell(1).setCellValue('Ignored')
            row(sheet, 3, 'Alan', 'Turing', 'Wilmslow', 'Male')
        })

        then:
        people*.firstName == ['Ada', 'Alan']
    }

    def 'skips rows that do not exist'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            row(sheet, 1, 'Ada', 'Lovelace', 'London', 'Female')
            row(sheet, 5, 'Alan', 'Turing', 'Wilmslow', 'Male')
        })

        then:
        people.size() == 2
    }

    def 'reads UTF-8 accents'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            row(sheet, 1, 'João', 'Conceição', 'Avenida São João', 'Male')
        })

        then:
        with(people[0]) {
            firstName == 'João'
            lastName == 'Conceição'
            address == 'Avenida São João'
        }
    }

    def 'imports many rows'() {
        when:
        def people = importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            (1..250).each { int index -> row(sheet, index, "Name$index", "Last$index", "Address $index", 'Male') }
        })

        then:
        people.size() == 250
        people[249].firstName == 'Name250'
    }

    def 'imports nothing from #description'() {
        expect:
        importer.importFile(workbook(fill)).empty

        where:
        description          | fill
        'a header-only sheet' | { Sheet sheet -> header(sheet) }
        'an empty sheet'      | { Sheet sheet -> }
    }

    def 'fails when a name cell is not text'() {
        when:
        importer.importFile(workbook { Sheet sheet ->
            header(sheet)
            def row = sheet.createRow(1)
            row.createCell(0).setCellValue(12345)
            row.createCell(1).setCellValue('Lovelace')
            row.createCell(2).setCellValue('London')
            row.createCell(3).setCellValue('Female')
        })

        then:
        thrown(IllegalStateException)
    }

    def 'fails when the content is not an xlsx file'() {
        when:
        importer.importFile(new ByteArrayInputStream('first_name,last_name\nAda,Lovelace\n'.bytes))

        then:
        thrown(Exception)
    }

    private static InputStream workbook(Closure fill) {
        new XSSFWorkbook().withCloseable { XSSFWorkbook workbook ->
            fill(workbook.createSheet('People'))

            def out = new ByteArrayOutputStream()
            workbook.write(out)
            new ByteArrayInputStream(out.toByteArray())
        }
    }

    private static void header(Sheet sheet) {
        row(sheet, 0, 'first_name', 'last_name', 'address', 'gender')
    }

    private static void row(Sheet sheet, int index, String... values) {
        def row = sheet.createRow(index)
        values.eachWithIndex { String value, int column -> row.createCell(column).setCellValue(value) }
    }
}
