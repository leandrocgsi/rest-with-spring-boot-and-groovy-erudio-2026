package br.com.erudio.integrationtests.support

import br.com.erudio.integrationtests.dto.BookDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.wrappers.json.WrapperBookDTO
import br.com.erudio.integrationtests.dto.wrappers.json.WrapperPersonDTO
import org.springframework.http.MediaType
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

class JsonCodec extends RepresentationCodec {

    private static final JsonMapper MAPPER = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    @Override
    String getMediaType() {
        MediaType.APPLICATION_JSON_VALUE
    }

    @Override
    <T> T read(String content, Class<T> type) {
        MAPPER.readValue(content, type)
    }

    @Override
    List<PersonDTO> people(String content) {
        read(content, WrapperPersonDTO).embedded.people
    }

    @Override
    List<BookDTO> books(String content) {
        read(content, WrapperBookDTO).embedded.books
    }
}
