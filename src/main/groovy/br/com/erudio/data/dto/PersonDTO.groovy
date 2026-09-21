package br.com.erudio.data.dto

import br.com.erudio.model.Book
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.EqualsAndHashCode
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = 'people')
@EqualsAndHashCode(callSuper = true)
class PersonDTO extends RepresentationModel<PersonDTO> implements Serializable {

    Long id
    String firstName
    String lastName
    String address
    String gender
    Boolean enabled

    String profileUrl
    String photoUrl

    @JsonIgnore
    List<Book> books

    @JsonIgnore
    String getName() {
        (firstName != null ? firstName : '') + (lastName != null ? " $lastName" : '')
    }
}
