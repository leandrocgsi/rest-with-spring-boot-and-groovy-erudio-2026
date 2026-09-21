package br.com.erudio.integrationtests.support

import br.com.erudio.integrationtests.dto.BookDTO
import br.com.erudio.integrationtests.dto.PersonDTO
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelBook
import br.com.erudio.integrationtests.dto.wrappers.xmlandyaml.PagedModelPerson
import org.springframework.http.MediaType
import tools.jackson.databind.DeserializationFeature
import tools.jackson.dataformat.xml.XmlMapper

class XmlCodec extends RepresentationCodec {

    private static final XmlMapper MAPPER = XmlMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    @Override
    String getMediaType() {
        MediaType.APPLICATION_XML_VALUE
    }

    @Override
    <T> T read(String content, Class<T> type) {
        MAPPER.readValue(content, type)
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
