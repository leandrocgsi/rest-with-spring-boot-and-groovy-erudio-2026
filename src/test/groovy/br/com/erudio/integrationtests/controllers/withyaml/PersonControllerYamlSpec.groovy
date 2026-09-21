package br.com.erudio.integrationtests.controllers.withyaml

import br.com.erudio.integrationtests.controllers.PersonControllerSpec
import br.com.erudio.integrationtests.support.RepresentationCodec
import br.com.erudio.integrationtests.support.YamlCodec
import org.yaml.snakeyaml.Yaml
import spock.lang.Stepwise

@Stepwise
class PersonControllerYamlSpec extends PersonControllerSpec {

    private static final RepresentationCodec CODEC = new YamlCodec()

    @Override
    RepresentationCodec getCodec() {
        CODEC
    }

    def 'HATEOAS and HAL links'() {
        given:
        Map<String, Object> parsedYaml = new Yaml().load(pageOfPeople())

        when:
        List<Map<String, Object>> content = parsedYaml['content'] as List
        Map<String, Object> page = parsedYaml['page'] as Map
        List<Map<String, String>> pageLinks = parsedYaml['links'] as List

        then:
        !content.empty
        verifyPersonLinks(content)

        page['number'] == 3
        page['size'] == 12
        page['totalElements'].toString() as int > 0
        page['totalPages'].toString() as int > 0

        !pageLinks.empty
        verifyPageLinks(pageLinks)
    }

    private static void verifyPersonLinks(List<Map<String, Object>> content) {
        content.each { Map<String, Object> person ->
            (person['links'] as List<Map<String, String>>).each { Map<String, String> link ->
                ['rel', 'href', 'type'].each { String attribute ->
                    assert link.containsKey(attribute): "HATEOAS/HAL link $attribute is missing"
                }
                assert link['href'] ==~ LINK_URL_PATTERN: "HATEOAS/HAL link $link has an invalid URL"
            }
        }
    }

    private static void verifyPageLinks(List<Map<String, String>> pageLinks) {
        pageLinks.each { Map<String, String> pageLink ->
            assert pageLink.containsKey('href'): 'HATEOAS/HAL page link href is missing'
            assert pageLink['href'] ==~ LINK_URL_PATTERN: "HATEOAS/HAL page link $pageLink has an invalid URL"
        }
    }
}
