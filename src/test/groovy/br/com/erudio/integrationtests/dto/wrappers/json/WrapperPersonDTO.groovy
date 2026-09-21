package br.com.erudio.integrationtests.dto.wrappers.json

import com.fasterxml.jackson.annotation.JsonProperty

class WrapperPersonDTO implements Serializable {

    @JsonProperty('_embedded')
    PersonEmbeddedDTO embedded
}
