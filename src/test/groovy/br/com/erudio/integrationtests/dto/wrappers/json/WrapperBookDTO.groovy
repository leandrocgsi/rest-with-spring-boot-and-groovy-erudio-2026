package br.com.erudio.integrationtests.dto.wrappers.json

import com.fasterxml.jackson.annotation.JsonProperty

class WrapperBookDTO implements Serializable {

    @JsonProperty('_embedded')
    BookEmbeddedDTO embedded
}
