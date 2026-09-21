package br.com.erudio.integrationtests.controllers

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.BookDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.support.RepresentationCodec
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationSpec
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.specification.RequestSpecification
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared
import spock.lang.Stepwise

import java.time.LocalDate

import static io.restassured.RestAssured.given

@Stepwise
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class BookControllerSpec extends AbstractIntegrationSpec {

    @Shared
    RequestSpecification specification

    @Shared
    BookDTO book = new BookDTO()

    @Shared
    TokenDTO tokenDto

    abstract RepresentationCodec getCodec()

    def 'signin'() {
        given:
        def credentials = new AccountCredentialsDTO('leandro', 'admin123')
        def signinSpecification = new RequestSpecBuilder()
            .setBasePath('/auth/signin')
            .setPort(TestConfigs.SERVER_PORT)
            .build()

        when:
        def content = codec.withBody(codec.request(signinSpecification), credentials)
        .when()
            .post()
        .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()
        tokenDto = codec.read(content, TokenDTO)

        specification = new RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, TestConfigs.ORIGIN_ERUDIO)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${tokenDto.accessToken}")
            .setBasePath('/api/book/v1')
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(new RequestLoggingFilter(LogDetail.ALL))
            .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
            .build()

        then:
        tokenDto.accessToken != null
        tokenDto.refreshToken != null
    }

    def 'create a book'() {
        given:
        mockBook()

        when:
        def content = codec.withBody(codec.request(specification), book)
        .when()
            .post()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        book = codec.read(content, BookDTO)

        then:
        with(book) {
            id != null
            title == 'Docker Deep Dive'
            author == 'Nigel Poulton'
            price == 55.99d
        }
    }

    def 'update a book'() {
        given:
        book.title = 'Docker Deep Dive - Updated'

        when:
        def content = codec.withBody(codec.request(specification), book)
        .when()
            .put()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        book = codec.read(content, BookDTO)

        then:
        with(book) {
            id != null
            id > 0
            title == 'Docker Deep Dive - Updated'
            author == 'Nigel Poulton'
            price == 55.99d
        }
    }

    def 'find a book by id'() {
        when:
        def content = codec.request(specification)
            .pathParam('id', book.id)
        .when()
            .get('{id}')
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        book = codec.read(content, BookDTO)

        then:
        with(book) {
            id != null
            id > 0
            title == 'Docker Deep Dive - Updated'
            author == 'Nigel Poulton'
            price == 55.99d
        }
    }

    def 'delete a book'() {
        expect:
        given(specification)
            .pathParam('id', book.id)
        .when()
            .delete('{id}')
        .then()
            .statusCode(204)
    }

    def 'find all books'() {
        when:
        def content = given(specification)
            .accept(codec.mediaType)
            .queryParams('page', 9, 'size', 12, 'direction', 'asc')
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        def books = codec.books(content)

        then:
        with(books[0]) {
            id != null
            title != null
            author != null
            price != null
            id > 0
            title == 'The Art of Agile Development'
            author == 'James Shore e Shane Warden'
            price == 97.21d
        }
        with(books[7]) {
            id != null
            title != null
            author != null
            price != null
            id > 0
            title == 'The Art of Computer Programming, Volume 1: Fundamental Algorithms'
            author == 'Donald E. Knuth'
            price == 139.69d
        }
    }

    private void mockBook() {
        book.title = 'Docker Deep Dive'
        book.author = 'Nigel Poulton'
        book.price = 55.99d
        book.launchDate = LocalDate.now()
    }
}
