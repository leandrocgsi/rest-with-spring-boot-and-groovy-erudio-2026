package br.com.erudio.integrationtests.controllers.file

import br.com.erudio.integrationtests.AuthenticatedIntegrationSpec

import java.nio.file.Files
import java.nio.file.Path

import static io.restassured.RestAssured.given
import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.endsWith
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.startsWith

class FileControllerSpec extends AuthenticatedIntegrationSpec {

    static final String BASE = '/api/file/v1'

    def 'upload stores the file and describes it'() {
        given:
        def name = uniqueName('.txt')
        def content = 'hello upload'.getBytes(UTF_8)

        expect:
        given().spec(authenticated())
            .multiPart('file', name, content, 'text/plain')
        .when()
            .post("$BASE/uploadFile")
        .then()
            .statusCode(200)
            .body('fileName', equalTo(name))
            .body('fileType', equalTo('text/plain'))
            .body('size', equalTo(content.length))
            .body('fileDownloadUri', endsWith("$BASE/downloadFile/$name"))
    }

    def 'an uploaded file can be downloaded with the same content'() {
        given:
        def name = uniqueName('.txt')
        def content = upload(name, 'Formação Spring Boot 2026'.getBytes(UTF_8), 'text/plain')

        when:
        def downloaded = given().spec(authenticated())
        .when()
            .get("$BASE/downloadFile/$name")
        .then()
            .statusCode(200)
            .header('Content-Disposition', equalTo("attachment; filename=\"$name\"".toString()))
            .contentType(startsWith('text/plain'))
        .extract()
            .asByteArray()

        then:
        downloaded == content
    }

    def 'binary files survive the round trip byte for byte'() {
        given:
        def name = uniqueName('.bin')
        def content = new byte[2 * 1024 * 1024]
        new Random(42).nextBytes(content)
        upload(name, content, 'application/octet-stream')

        when:
        def downloaded = given().spec(authenticated())
        .when()
            .get("$BASE/downloadFile/$name")
        .then()
            .statusCode(200)
            .contentType('application/octet-stream')
        .extract()
            .asByteArray()

        then:
        downloaded == content
    }

    def 'the content type of the download follows the file extension'() {
        given:
        def csv = uniqueName('.csv')
        upload(csv, 'a,b\n1,2\n'.getBytes(UTF_8), 'text/csv')

        expect:
        given().spec(authenticated())
        .when()
            .get("$BASE/downloadFile/$csv")
        .then()
            .statusCode(200)
            .contentType(startsWith('text/csv'))
    }

    def 'uploading an existing name replaces the file'() {
        given:
        def name = uniqueName('.txt')
        upload(name, 'first version'.getBytes(UTF_8), 'text/plain')
        def second = upload(name, 'second version'.getBytes(UTF_8), 'text/plain')

        when:
        def downloaded = given().spec(authenticated())
        .when()
            .get("$BASE/downloadFile/$name")
        .then()
            .statusCode(200)
        .extract()
            .asByteArray()

        then:
        downloaded == second
    }

    def 'uploadMultipleFiles stores all of them and returns one description each'() {
        given:
        def first = uniqueName('.txt')
        def second = uniqueName('.csv')

        when:
        given().spec(authenticated())
            .multiPart('files', first, 'one'.getBytes(UTF_8), 'text/plain')
            .multiPart('files', second, 'a,b\n'.getBytes(UTF_8), 'text/csv')
        .when()
            .post("$BASE/uploadMultipleFiles")
        .then()
            .statusCode(200)
            .body('size()', equalTo(2))
            .body('fileName', contains(first, second))
            .body('fileType', contains('text/plain', 'text/csv'))
            .body('fileDownloadUri', contains(
                endsWith("$BASE/downloadFile/$first"),
                endsWith("$BASE/downloadFile/$second")))

        then:
        [first, second].every { String name ->
            given().spec(authenticated()).when().get("$BASE/downloadFile/$name").statusCode() == 200
        }
    }

    def 'downloading a file that does not exist is not found'() {
        given:
        def name = uniqueName('.txt')

        expect:
        given().spec(authenticated())
        .when()
            .get("$BASE/downloadFile/$name")
        .then()
            .statusCode(404)
            .body('message', equalTo("File not found $name".toString()))
    }

    def 'a file name that escapes the upload directory is rejected and nothing is written'() {
        given:
        def tag = UUID.randomUUID().toString()
        def name = "../escaped-${tag}.txt"

        when:
        given().spec(authenticated())
            .multiPart('file', name, 'boom'.getBytes(UTF_8), 'text/plain')
        .when()
            .post("$BASE/uploadFile")
        .then()
            .statusCode(500)
            .body('message', equalTo("Could not store file $name. Please try Again!".toString()))

        then:
        !Files.exists(Path.of('build', "escaped-${tag}.txt"))
    }

    def 'upload without the file part is a bad request'() {
        expect:
        given().spec(authenticated())
            .multiPart('somethingElse', uniqueName('.txt'), 'x'.getBytes(UTF_8), 'text/plain')
        .when()
            .post("$BASE/uploadFile")
        .then()
            .statusCode(400)
    }

    def 'upload and download require authentication'() {
        expect:
        given().spec(anonymous())
            .multiPart('file', uniqueName('.txt'), 'x'.getBytes(UTF_8), 'text/plain')
        .when()
            .post("$BASE/uploadFile")
        .then()
            .statusCode(403)

        and:
        given().spec(anonymous())
            .multiPart('files', uniqueName('.txt'), 'x'.getBytes(UTF_8), 'text/plain')
        .when()
            .post("$BASE/uploadMultipleFiles")
        .then()
            .statusCode(403)

        and:
        given().spec(anonymous())
        .when()
            .get("$BASE/downloadFile/whatever.txt")
        .then()
            .statusCode(403)
    }

    private static String uniqueName(String extension) {
        "${UUID.randomUUID()}$extension"
    }

    private static byte[] upload(String name, byte[] content, String contentType) {
        given().spec(authenticated())
            .multiPart('file', name, content, contentType)
        .when()
            .post("$BASE/uploadFile")
        .then()
            .statusCode(200)
        content
    }
}
