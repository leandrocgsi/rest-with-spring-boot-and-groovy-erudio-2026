package br.com.erudio.integrationtests.controllers

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.PersonDTO
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

import static io.restassured.RestAssured.given

@Stepwise
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class PersonControllerSpec extends AbstractIntegrationSpec {

    protected static final String LINK_URL_PATTERN = /https?:\/\/.+\/api\/person\/v1.*/

    @Shared
    RequestSpecification specification

    @Shared
    PersonDTO person = new PersonDTO()

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
            .setBasePath('/api/person/v1')
            .setPort(TestConfigs.SERVER_PORT)
            .addFilter(new RequestLoggingFilter(LogDetail.ALL))
            .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
            .build()

        then:
        tokenDto.accessToken != null
        tokenDto.refreshToken != null
    }

    def 'create a person'() {
        given:
        mockPerson()

        when:
        def content = codec.withBody(codec.request(specification), person)
        .when()
            .post()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        person = codec.read(content, PersonDTO)

        then:
        with(person) {
            id != null
            id > 0
            firstName == 'Linus'
            lastName == 'Torvalds'
            address == 'Helsinki - Finland'
            gender == 'Male'
            enabled
        }
    }

    def 'update a person'() {
        given:
        person.lastName = 'Benedict Torvalds'

        when:
        def content = codec.withBody(codec.request(specification), person)
        .when()
            .put()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        person = codec.read(content, PersonDTO)

        then:
        with(person) {
            id != null
            id > 0
            firstName == 'Linus'
            lastName == 'Benedict Torvalds'
            address == 'Helsinki - Finland'
            gender == 'Male'
            enabled
        }
    }

    def 'find a person by id'() {
        when:
        def content = codec.request(specification)
            .pathParam('id', person.id)
        .when()
            .get('{id}')
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        person = codec.read(content, PersonDTO)

        then:
        with(person) {
            id != null
            id > 0
            firstName == 'Linus'
            lastName == 'Benedict Torvalds'
            address == 'Helsinki - Finland'
            gender == 'Male'
            enabled
        }
    }

    def 'disable a person'() {
        when:
        def content = codec.request(specification)
            .pathParam('id', person.id)
        .when()
            .patch('{id}')
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        person = codec.read(content, PersonDTO)

        then:
        with(person) {
            id != null
            id > 0
            firstName == 'Linus'
            lastName == 'Benedict Torvalds'
            address == 'Helsinki - Finland'
            gender == 'Male'
            !enabled
        }
    }

    def 'delete a person'() {
        expect:
        given(specification)
            .pathParam('id', person.id)
        .when()
            .delete('{id}')
        .then()
            .statusCode(204)
    }

    def 'find all people'() {
        when:
        def content = given(specification)
            .accept(codec.mediaType)
            .queryParams('page', 3, 'size', 12, 'direction', 'asc')
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        def people = codec.people(content)

        then:
        with(people[0]) {
            id != null
            id > 0
            firstName == 'Allin'
            lastName == 'Emmot'
            address == '7913 Lindbergh Way'
            gender == 'Male'
            !enabled
        }
        with(people[4]) {
            id != null
            id > 0
            firstName == 'Alonso'
            lastName == 'Luchelli'
            address == '9 Doe Crossing Avenue'
            gender == 'Male'
            !enabled
        }
    }

    def 'find people by name'() {
        when:
        def content = given(specification)
            .accept(codec.mediaType)
            .pathParam('firstName', 'and')
            .queryParams('page', 0, 'size', 12, 'direction', 'asc')
        .when()
            .get('findPeopleByName/{firstName}')
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
        def people = codec.people(content)

        then:
        with(people[0]) {
            id != null
            id > 0
            firstName == 'Alessandro'
            lastName == 'McFaul'
            address == '5 Lukken Plaza'
            gender == 'Male'
            enabled
        }
        with(people[4]) {
            id != null
            id > 0
            firstName == 'Brandyn'
            lastName == 'Grasha'
            address == '96 Mosinee Parkway'
            gender == 'Male'
            enabled
        }
    }

    protected String pageOfPeople() {
        given(specification)
            .accept(codec.mediaType)
            .queryParams('page', 3, 'size', 12, 'direction', 'asc')
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .body()
            .asString()
    }

    private void mockPerson() {
        person.firstName = 'Linus'
        person.lastName = 'Torvalds'
        person.address = 'Helsinki - Finland'
        person.gender = 'Male'
        person.enabled = true
        person.profileUrl = 'https://pub.erudio.com.br/meus-cursos'
        person.photoUrl = 'https://pub.erudio.com.br/meus-cursos'
    }
}
