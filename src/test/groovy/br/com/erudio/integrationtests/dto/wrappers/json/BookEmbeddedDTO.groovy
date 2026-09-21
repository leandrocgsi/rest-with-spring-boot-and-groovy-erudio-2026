package br.com.erudio.integrationtests.dto.wrappers.json

import br.com.erudio.integrationtests.dto.BookDTO
import com.fasterxml.jackson.annotation.JsonProperty

class BookEmbeddedDTO implements Serializable {

    @JsonProperty('books')
    List<BookDTO> books
}
