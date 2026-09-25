package br.com.erudio.file.importer.impl

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.importer.contract.FileImporter
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component

@Component
class XlsxImporter implements FileImporter {

    @Override
    List<PersonDTO> importFile(InputStream inputStream) {
        new XSSFWorkbook(inputStream).withCloseable { workbook ->
            workbook.getSheetAt(0).toList()
                .drop(1)
                .findAll { isRowValid(it) }
                .collect { parseRowToPersonDto(it) }
        }
    }

    private static PersonDTO parseRowToPersonDto(Row row) {
        new PersonDTO(
            firstName: row.getCell(0).stringCellValue,
            lastName: row.getCell(1).stringCellValue,
            address: row.getCell(2).stringCellValue,
            gender: row.getCell(3).stringCellValue,
            enabled: true
        )
    }

    private static boolean isRowValid(Row row) {
        Cell firstCell = row.getCell(0)
        firstCell != null && firstCell.cellType != CellType.BLANK
    }
}
