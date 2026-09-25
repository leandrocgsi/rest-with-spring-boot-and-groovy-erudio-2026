package br.com.erudio.file.exporter.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.contract.PersonExporter
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component

@Component
class XlsxExporter implements PersonExporter {

    private static final List<String> HEADERS = ['ID', 'First Name', 'Last Name', 'Address', 'Gender', 'Enabled']

    @Override
    Resource exportPeople(List<PersonDTO> people) {
        new XSSFWorkbook().withCloseable { workbook ->
            Sheet sheet = workbook.createSheet('People')

            Row headerRow = sheet.createRow(0)
            CellStyle headerStyle = createHeaderCellStyle(workbook)
            HEADERS.eachWithIndex { header, column ->
                def cell = headerRow.createCell(column)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            people.eachWithIndex { person, index ->
                Row row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(person.id as double)
                row.createCell(1).setCellValue(person.firstName)
                row.createCell(2).setCellValue(person.lastName)
                row.createCell(3).setCellValue(person.address)
                row.createCell(4).setCellValue(person.gender)
                row.createCell(5).setCellValue(person.enabled ? 'Yes' : 'No')
            }

            HEADERS.indices.each { sheet.autoSizeColumn(it) }

            def outputStream = new ByteArrayOutputStream()
            workbook.write(outputStream)

            new ByteArrayResource(outputStream.toByteArray())
        }
    }

    @Override
    Resource exportPerson(PersonDTO person) {
        null
    }

    private static CellStyle createHeaderCellStyle(Workbook workbook) {
        Font font = workbook.createFont()
        font.bold = true

        CellStyle style = workbook.createCellStyle()
        style.font = font
        style.alignment = HorizontalAlignment.CENTER
        style
    }
}
