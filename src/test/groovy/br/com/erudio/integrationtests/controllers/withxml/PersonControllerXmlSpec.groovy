package br.com.erudio.integrationtests.controllers.withxml

import br.com.erudio.integrationtests.controllers.PersonControllerSpec
import br.com.erudio.integrationtests.support.RepresentationCodec
import br.com.erudio.integrationtests.support.XmlCodec
import io.restassured.path.xml.XmlPath
import spock.lang.Stepwise

@Stepwise
class PersonControllerXmlSpec extends PersonControllerSpec {

    private static final RepresentationCodec CODEC = new XmlCodec()

    @Override
    RepresentationCodec getCodec() {
        CODEC
    }

    def 'HATEOAS and HAL links'() {
        given:
        def xmlPath = new XmlPath(pageOfPeople())

        when:
        List<String> peopleLinks = xmlPath.getList('PagedModel.content.content.links.href')
        List<String> pageLinks = xmlPath.getList('PagedModel.links.href')

        then:
        !peopleLinks.empty
        peopleLinks.every { String link -> link ==~ LINK_URL_PATTERN }

        !pageLinks.empty
        pageLinks.every { String link -> link ==~ LINK_URL_PATTERN }

        xmlPath.getString('PagedModel.page.size') as int == 12
        xmlPath.getString('PagedModel.page.number') as int == 3
        xmlPath.getString('PagedModel.page.totalElements') as int > 0
        xmlPath.getString('PagedModel.page.totalPages') as int > 0
    }
}
