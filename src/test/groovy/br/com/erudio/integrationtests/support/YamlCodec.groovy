package br.com.erudio.integrationtests.support

import br.com.erudio.integrationtests.controllers.withyaml.mapper.YAMLMapper
import br.com.erudio.integrationtests.dto.BookDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelBook
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelPerson
import io.restassured.config.EncoderConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.springframework.http.MediaType

class YamlCodec extends RepresentationCodec {

    private static final YAMLMapper MAPPER = new YAMLMapper()

    @Override
    String getMediaType() {
        MediaType.APPLICATION_YAML_VALUE
    }

    @Override
    RequestSpecification request(RequestSpecification base) {
        super.request(base).config(
            RestAssuredConfig.config().encoderConfig(
                EncoderConfig.encoderConfig().encodeContentTypeAs(mediaType, ContentType.TEXT)))
    }

    @Override
    RequestSpecification withBody(RequestSpecification request, Object body) {
        request.body(body, MAPPER)
    }

    @Override
    <T> T read(String content, Class<T> type) {
        MAPPER.read(content, type)
    }

    @Override
    List<PersonDTO> people(String content) {
        read(content, PagedModelPerson).content
    }

    @Override
    List<BookDTO> books(String content) {
        read(content, PagedModelBook).content
    }
}
