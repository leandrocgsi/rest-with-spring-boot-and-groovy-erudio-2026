package br.com.erudio.integrationtests.dto.wrappers.json

import br.com.erudio.integrationtests.dto.PersonDTO
import com.fasterxml.jackson.annotation.JsonProperty

class PersonEmbeddedDTO {

    @JsonProperty('people')
    List<PersonDTO> people
}
