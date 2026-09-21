package br.com.erudio.integrationtests.dto.wrappers.json

import br.com.erudio.integrationtests.dto.PersonDTO
import com.fasterxml.jackson.annotation.JsonProperty

class PersonEmbeddedDTO implements Serializable {

    @JsonProperty('people')
    List<PersonDTO> people
}
