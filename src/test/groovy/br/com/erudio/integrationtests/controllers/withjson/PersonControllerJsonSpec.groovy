package br.com.erudio.integrationtests.controllers.withjson

import br.com.erudio.integrationtests.controllers.PersonControllerSpec
import br.com.erudio.integrationtests.support.JsonCodec
import br.com.erudio.integrationtests.support.RepresentationCodec
import spock.lang.Stepwise

import static io.restassured.RestAssured.given

@Stepwise
class PersonControllerJsonSpec extends PersonControllerSpec {

    private static final RepresentationCodec CODEC = new JsonCodec()

    @Override
    RepresentationCodec getCodec() {
        CODEC
    }

    def 'HATEOAS and HAL links'() {
        given:
        def response = given(specification)
            .accept(codec.mediaType)
            .queryParams('page', 3, 'size', 12, 'direction', 'asc')
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(codec.mediaType)
            .extract()
            .response()

        when:
        List<Map> people = response.jsonPath().getList('_embedded.people')
        Map pageLinks = response.jsonPath().getMap('_links')
        Map page = response.jsonPath().getMap('page')

        then:
        !people.empty
        verifyPersonLinks(people)

        pageLinks.keySet().containsAll(['self', 'first', 'prev', 'next', 'last'])

        page['size'] == 12
        page['number'] == 3
        page['totalElements'] > 0
        page['totalPages'] > 0
    }

    private static void verifyPersonLinks(List<Map> people) {
        people.each { Map person ->
            Map<String, Map> links = person['_links'] as Map

            ['self', 'findAll', 'findByName', 'create', 'update', 'delete', 'disable', 'massCreation', 'exportPage'].each { String rel ->
                assert links.containsKey(rel): "HATEOAS/HAL link '$rel' is missing"
            }

            links.each { String rel, Map link ->
                assert link['href'] ==~ LINK_URL_PATTERN: "HATEOAS/HAL link $rel has an invalid URL"
                assert link['type'] != null: "HATEOAS/HAL link $rel has an invalid HTTP method"
            }
        }
    }
}
