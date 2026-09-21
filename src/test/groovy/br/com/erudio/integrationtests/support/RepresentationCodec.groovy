package br.com.erudio.integrationtests.support

import br.com.erudio.integrationtests.dto.BookDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import io.restassured.specification.RequestSpecification

import static io.restassured.RestAssured.given

abstract class RepresentationCodec {

    abstract String getMediaType()

    abstract <T> T read(String content, Class<T> type)

    abstract List<PersonDTO> people(String content)

    abstract List<BookDTO> books(String content)

    RequestSpecification request(RequestSpecification base) {
        given().spec(base).contentType(mediaType).accept(mediaType)
    }

    RequestSpecification withBody(RequestSpecification request, Object body) {
        request.body(body)
    }
}
