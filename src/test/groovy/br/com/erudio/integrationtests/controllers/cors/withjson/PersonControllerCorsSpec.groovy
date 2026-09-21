package br.com.erudio.integrationtests.controllers.cors.withjson

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.support.JsonCodec
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationSpec
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.LogDetail
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.specification.RequestSpecification
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import spock.lang.Shared
import spock.lang.Stepwise

import static io.restassured.RestAssured.given

@Stepwise
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PersonControllerCorsSpec extends AbstractIntegrationSpec {

    @Shared
    RequestSpecification specification

    @Shared
    PersonDTO person = new PersonDTO()

    @Shared
    TokenDTO tokenDto

    def codec = new JsonCodec()

    def 'signin'() {
        when:
        tokenDto = given()
            .basePath('/auth/signin')
            .port(TestConfigs.SERVER_PORT)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(new AccountCredentialsDTO('leandro', 'admin123'))
        .when()
            .post()
        .then()
            .statusCode(200)
            .extract()
            .body()
            .as(TokenDTO)

        then:
        tokenDto.accessToken != null
        tokenDto.refreshToken != null
    }

    def 'create a person from an allowed origin'() {
        given:
        mockPerson()
        specification = specificationFor(TestConfigs.ORIGIN_ERUDIO)

        when:
        def content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(person)
        .when()
            .post()
        .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()
        person = codec.read(content, PersonDTO)

        then:
        with(person) {
            id != null
            firstName != null
            lastName != null
            address != null
            gender != null
            id > 0
            firstName == 'Richard'
            lastName == 'Stallman'
            address == 'New York City - New York - USA'
            gender == 'Male'
            enabled
        }
    }

    def 'creating a person from a wrong origin is rejected'() {
        given:
        specification = specificationFor(TestConfigs.ORIGIN_SEMERU)

        when:
        def content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(person)
        .when()
            .post()
        .then()
            .statusCode(403)
            .extract()
            .body()
            .asString()

        then:
        content == 'Invalid CORS request'
    }

    def 'find a person by id from an allowed origin'() {
        given:
        specification = specificationFor(TestConfigs.ORIGIN_LOCAL)

        when:
        def content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam('id', person.id)
        .when()
            .get('{id}')
        .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()
        person = codec.read(content, PersonDTO)

        then:
        with(person) {
            id != null
            firstName != null
            lastName != null
            address != null
            gender != null
            id > 0
            firstName == 'Richard'
            lastName == 'Stallman'
            address == 'New York City - New York - USA'
            gender == 'Male'
            enabled
        }
    }

    def 'finding a person by id from a wrong origin is rejected'() {
        given:
        specification = specificationFor(TestConfigs.ORIGIN_SEMERU)

        when:
        def content = given(specification)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .pathParam('id', person.id)
        .when()
            .get('{id}')
        .then()
            .statusCode(403)
            .extract()
            .body()
            .asString()

        then:
        content == 'Invalid CORS request'
    }

    private RequestSpecification specificationFor(String origin) {
        new RequestSpecBuilder()
            .addHeader(TestConfigs.HEADER_PARAM_ORIGIN, origin)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${tokenDto.accessToken}")
            .setBasePath('/api/person/v1')
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(new RequestLoggingFilter(LogDetail.ALL))
            .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
            .build()
    }

    private void mockPerson() {
        person.firstName = 'Richard'
        person.lastName = 'Stallman'
        person.address = 'New York City - New York - USA'
        person.gender = 'Male'
        person.enabled = true
    }
}
