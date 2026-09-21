package br.com.erudio.integrationtests.controllers.report

import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.integrationtests.AuthenticatedIntegrationSpec
import br.com.erudio.testsupport.NetworkAssumptions
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import io.restassured.path.json.JsonPath
import io.restassured.response.ValidatableResponse
import org.apache.commons.csv.CSVFormat
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.hamcrest.Matchers
import spock.lang.Requires

import static io.restassured.RestAssured.given
import static java.nio.charset.StandardCharsets.US_ASCII
import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.emptyOrNullString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.startsWith

class PersonControllerFilesSpec extends AuthenticatedIntegrationSpec {

    static final String BASE = '/api/person/v1'
    static final String XLSX = MediaTypes.APPLICATION_XLSX_VALUE
    static final String CSV = MediaTypes.APPLICATION_CSV_VALUE
    static final String PDF = MediaTypes.APPLICATION_PDF_VALUE
    static final List<String> EXPORT_HEADER = ['ID', 'First Name', 'Last Name', 'Address', 'Gender', 'Enabled']

    List<Long> createdIds = []

    def cleanup() {
        createdIds.each { Long id -> given().spec(authenticated()).delete("$BASE/$id").then().statusCode(204) }
        createdIds.clear()
    }

    def 'exportPage as CSV has one line per person of the page in the same order as the listing'() {
        when:
        def records = parseCsv(export(CSV, 0, 5, 'asc'), EXPORT_HEADER)

        then:
        records.size() == 5
        records*.get('ID') == idsOfThePage(0, 5, 'asc')*.toString()
        records*.get('First Name') == firstNamesOfThePage(0, 5, 'asc')
    }

    def 'exportPage as CSV has the headers and the download name'() {
        expect:
        given().spec(authenticated())
            .header('Accept', CSV)
            .queryParam('size', 2)
        .when()
            .get("$BASE/exportPage")
        .then()
            .statusCode(200)
            .contentType(startsWith('text/csv'))
            .header('Content-Disposition', equalTo('attachment; filename="people_exported.csv"'))
            .body(startsWith('ID,First Name,Last Name,Address,Gender,Enabled'))
    }

    def 'exportPage respects the page size and the direction'() {
        when:
        def ascending = parseCsv(export(CSV, 0, 3, 'asc'), EXPORT_HEADER)
        def descending = parseCsv(export(CSV, 0, 3, 'desc'), EXPORT_HEADER)
        def secondPage = parseCsv(export(CSV, 1, 3, 'asc'), EXPORT_HEADER)

        then:
        ascending.size() == 3
        descending.size() == 3
        secondPage.size() == 3
        ascending[0].get('ID') != descending[0].get('ID')
        descending*.get('ID') == idsOfThePage(0, 3, 'desc')*.toString()
        secondPage*.get('ID') == idsOfThePage(1, 3, 'asc')*.toString()
    }

    def 'exportPage outside the data is just the header'() {
        expect:
        parseCsv(export(CSV, 99999, 5, 'asc'), EXPORT_HEADER).empty
    }

    def 'exportPage as XLSX has the same rows as the listing'() {
        given:
        def formatter = new DataFormatter()
        def expectedNames = firstNamesOfThePage(0, 5, 'asc')
        def expectedIds = idsOfThePage(0, 5, 'asc')

        when:
        def workbook = WorkbookFactory.create(new ByteArrayInputStream(export(XLSX, 0, 5, 'asc')))
        def sheet = workbook.getSheet('People')

        then:
        sheet != null
        sheet.lastRowNum == 5
        sheet.getRow(0).collect { cell -> formatter.formatCellValue(cell) } == EXPORT_HEADER
        (0..<5).every { int index ->
            def row = sheet.getRow(index + 1)
            row.getCell(0).numericCellValue == expectedIds[index].doubleValue() &&
                formatter.formatCellValue(row.getCell(1)) == expectedNames[index] &&
                formatter.formatCellValue(row.getCell(5)) in ['Yes', 'No']
        }

        cleanup:
        workbook?.close()
    }

    def 'exportPage as XLSX has the content type and the download name'() {
        expect:
        given().spec(authenticated())
            .header('Accept', XLSX)
            .queryParam('size', 2)
        .when()
            .get("$BASE/exportPage")
        .then()
            .statusCode(200)
            .contentType(XLSX)
            .header('Content-Disposition', equalTo('attachment; filename="people_exported.xlsx"'))
    }

    @Requires({ NetworkAssumptions.reportImagesAreReachable() })
    def 'exportPage as PDF is a valid PDF with the people of the page'() {
        when:
        def pdf = given().spec(authenticated())
            .header('Accept', PDF)
            .queryParam('page', 0).queryParam('size', 5)
        .when()
            .get("$BASE/exportPage")
        .then()
            .statusCode(200)
            .contentType(PDF)
            .header('Content-Disposition', equalTo('attachment; filename="people_exported.pdf"'))
        .extract().asByteArray()
        def text = pdfText(pdf)

        then:
        new String(pdf, 0, 4, US_ASCII) == '%PDF'
        text.contains('PEOPLE REPORT')
        firstNamesOfThePage(0, 5, 'asc').every { String firstName -> text.contains(firstName) }
    }

    @Requires({ NetworkAssumptions.reportImagesAreReachable() })
    def 'export one person as PDF is a valid PDF about that person'() {
        given:
        String firstName = given().spec(authenticated()).accept('application/json').get("$BASE/1").then().statusCode(200).extract().path('firstName')

        when:
        def pdf = given().spec(authenticated())
            .header('Accept', PDF)
        .when()
            .get("$BASE/export/1")
        .then()
            .statusCode(200)
            .contentType(PDF)
            .header('Content-Disposition', containsString('.pdf'))
        .extract().asByteArray()

        then:
        new String(pdf, 0, 4, US_ASCII) == '%PDF'
        pdfText(pdf).contains(firstName)
    }

    def 'export one person that does not exist is not found'() {
        expect:
        given().spec(authenticated())
            .header('Accept', "$PDF, application/json")
        .when()
            .get("$BASE/export/999999")
        .then()
            .statusCode(404)
            .body('message', equalTo('No records found for this ID!'))
    }

    def 'export one person that does not exist asking only for PDF gets an empty forbidden'() {
        expect:
        given().spec(authenticated())
            .header('Accept', PDF)
        .when()
            .get("$BASE/export/999999")
        .then()
            .statusCode(403)
            .body(emptyOrNullString())
    }

    def 'a single person only exports to PDF'() {
        expect:
        given().spec(authenticated())
            .header('Accept', CSV)
        .when()
            .get("$BASE/export/1")
        .then()
            .statusCode(406)
    }

    def 'exportPage rejects a format it cannot produce'() {
        expect:
        given().spec(authenticated())
            .header('Accept', 'application/json')
        .when()
            .get("$BASE/exportPage")
        .then()
            .statusCode(Matchers.both(Matchers.greaterThanOrEqualTo(400)).and(Matchers.lessThan(500)))
    }

    def 'exports require authentication'() {
        expect:
        given().spec(anonymous()).header('Accept', CSV).get("$BASE/exportPage").then().statusCode(403)
        given().spec(anonymous()).header('Accept', PDF).get("$BASE/export/1").then().statusCode(403)
    }

    def 'massCreation from a CSV creates every person of the file'() {
        given:
        def tag = tag()
        def csv = 'first_name,last_name,address,gender\n' +
            "Imp${tag}A,Csv,Street 1,Female\n" +
            "Imp${tag}B,Csv,\"Street 2, apto 3\",Male\n"

        when:
        def created = massCreation('people.csv', csv.getBytes(UTF_8), 'text/csv')
            .statusCode(200)
            .body('size()', equalTo(2))
            .body('firstName', Matchers.contains("Imp${tag}A".toString(), "Imp${tag}B".toString()))
            .body('lastName', Matchers.everyItem(equalTo('Csv')))
            .body('address', Matchers.contains('Street 1', 'Street 2, apto 3'))
            .body('gender', Matchers.contains('Female', 'Male'))
            .body('enabled', Matchers.everyItem(equalTo(true)))
            .body('id', Matchers.everyItem(notNullValue()))
            .body('links', Matchers.everyItem(Matchers.not(Matchers.empty())))
        remember(created)

        then:
        peopleNamed("imp$tag") == 2
    }

    def 'massCreation from a CSV keeps the accents'() {
        given:
        def tag = tag()
        def csv = "first_name,last_name,address,gender\nJoão$tag,Conceição,Avenida São João,Male\n"

        when:
        def created = massCreation('people.csv', csv.getBytes(UTF_8), 'text/csv')
            .statusCode(200)
            .body('[0].firstName', equalTo("João$tag".toString()))
            .body('[0].lastName', equalTo('Conceição'))
            .body('[0].address', equalTo('Avenida São João'))
        remember(created)

        then:
        given().spec(authenticated()).accept('application/json')
            .get("$BASE/${created.extract().path('[0].id')}")
            .then().statusCode(200)
            .body('firstName', equalTo("João$tag".toString()))
            .body('address', equalTo('Avenida São João'))
    }

    def 'massCreation from a CSV with a missing column creates nobody'() {
        given:
        def tag = tag()
        def csv = "first_name,last_name,address\nImp$tag,Csv,Street 1\n"

        when:
        massCreation('people.csv', csv.getBytes(UTF_8), 'text/csv')
            .statusCode(500)
            .body('message', equalTo('Error processing the file!'))

        then:
        peopleNamed("imp$tag") == 0
    }

    def 'massCreation from an XLSX creates every person of the file'() {
        given:
        def tag = tag()
        def workbook = xlsx([
            ['first_name', 'last_name', 'address', 'gender'],
            ["Imp${tag}A", 'Xlsx', 'Street 1', 'Female'],
            ["Imp${tag}B", 'Xlsx', 'Street 2', 'Male'],
            ["Imp${tag}C", 'Xlsx', 'Street 3', 'Female']
        ])

        when:
        def created = massCreation('people.xlsx', workbook, XLSX)
            .statusCode(200)
            .body('size()', equalTo(3))
            .body('firstName', Matchers.contains("Imp${tag}A".toString(), "Imp${tag}B".toString(), "Imp${tag}C".toString()))
            .body('lastName', Matchers.everyItem(equalTo('Xlsx')))
            .body('enabled', Matchers.everyItem(equalTo(true)))
            .body('id', Matchers.everyItem(notNullValue()))
        remember(created)

        then:
        peopleNamed("imp$tag") == 3
    }

    def 'massCreation can answer in XML'() {
        given:
        def tag = tag()
        def workbook = xlsx([
            ['first_name', 'last_name', 'address', 'gender'],
            ["Imp$tag", 'Xml', 'Street 1', 'Male']
        ])

        when:
        def response = given().spec(authenticated())
            .header('Accept', 'application/xml')
            .multiPart('file', 'people.xlsx', workbook, XLSX)
        .when()
            .post("$BASE/massCreation")
        .then()
            .statusCode(200)
            .contentType(containsString('xml'))
            .body('List.item.firstName', equalTo("Imp$tag".toString()))
        createdIds.addAll(response.extract().xmlPath().getList('List.item.id', Long))

        then:
        createdIds.size() == 1
    }

    def 'massCreation of an unsupported file type is rejected'() {
        expect:
        massCreation('people.txt', 'first_name,last_name,address,gender\nX,Y,Z,Male\n'.getBytes(UTF_8), 'text/plain')
            .statusCode(500)
            .body('message', equalTo('Error processing the file!'))
    }

    def 'massCreation of an empty file is a bad request'() {
        expect:
        massCreation('people.csv', new byte[0], 'text/csv')
            .statusCode(400)
            .body('message', equalTo('Please set a Valid File!'))
    }

    def 'massCreation without a file is rejected'() {
        expect:
        given().spec(authenticated())
            .multiPart('notTheFile', 'people.csv', 'x'.getBytes(UTF_8), 'text/csv')
        .when()
            .post("$BASE/massCreation")
        .then()
            .statusCode(400)
    }

    def 'massCreation requires authentication'() {
        expect:
        given().spec(anonymous())
            .multiPart('file', 'people.csv', 'x'.getBytes(UTF_8), 'text/csv')
        .when()
            .post("$BASE/massCreation")
        .then()
            .statusCode(403)
    }

    private static List<Integer> idsOfThePage(int page, int size, String direction) {
        pageOfPeople(page, size, direction).getList('_embedded.people.id')
    }

    private static List<String> firstNamesOfThePage(int page, int size, String direction) {
        pageOfPeople(page, size, direction).getList('_embedded.people.firstName')
    }

    private static JsonPath pageOfPeople(int page, int size, String direction) {
        JsonPath.from(given().spec(authenticated())
            .queryParam('page', page).queryParam('size', size).queryParam('direction', direction)
            .accept('application/json')
        .when()
            .get(BASE)
        .then()
            .statusCode(200)
        .extract().asString())
    }

    private static byte[] export(String accept, int page, int size, String direction) {
        given().spec(authenticated())
            .header('Accept', accept)
            .queryParam('page', page).queryParam('size', size).queryParam('direction', direction)
        .when()
            .get("$BASE/exportPage")
        .then()
            .statusCode(200)
        .extract().asByteArray()
    }

    private static List parseCsv(byte[] csv, List<String> expectedHeader) {
        new InputStreamReader(new ByteArrayInputStream(csv), UTF_8).withCloseable { reader ->
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader).withCloseable { parser ->
                assert parser.headerNames == expectedHeader
                parser.records
            }
        }
    }

    private static String pdfText(byte[] pdf) {
        def reader = new PdfReader(pdf)
        try {
            def extractor = new PdfTextExtractor(reader)
            (1..reader.numberOfPages).collect { int page -> extractor.getTextFromPage(page) + '\n' }.join()
        } finally {
            reader.close()
        }
    }

    private static String tag() {
        UUID.randomUUID().toString().replace('-', '').substring(0, 10)
    }

    private static byte[] xlsx(List<List<String>> rows) {
        new XSSFWorkbook().withCloseable { XSSFWorkbook workbook ->
            def sheet = workbook.createSheet('People')
            rows.eachWithIndex { List<String> values, int r ->
                def row = sheet.createRow(r)
                values.eachWithIndex { String value, int c -> row.createCell(c).setCellValue(value) }
            }
            def out = new ByteArrayOutputStream()
            workbook.write(out)
            out.toByteArray()
        }
    }

    private static ValidatableResponse massCreation(String name, byte[] content, String contentType) {
        given().spec(authenticated())
            .multiPart('file', name, content, contentType)
        .when()
            .post("$BASE/massCreation")
        .then()
    }

    private void remember(ValidatableResponse created) {
        createdIds.addAll(created.extract().jsonPath().getList('id', Long))
    }

    private static int peopleNamed(String tag) {
        given().spec(authenticated())
            .accept('application/json')
        .when()
            .get("$BASE/findPeopleByName/$tag")
        .then()
            .statusCode(200)
        .extract().path('page.totalElements')
    }
}
