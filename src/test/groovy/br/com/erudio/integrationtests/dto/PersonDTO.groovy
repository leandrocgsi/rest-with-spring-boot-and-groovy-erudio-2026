package br.com.erudio.integrationtests.dto

import br.com.erudio.model.Book
import groovy.transform.EqualsAndHashCode
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.XmlTransient

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode
class PersonDTO {

    Long id
    String firstName
    String lastName
    String address
    String gender
    Boolean enabled

    String profileUrl
    String photoUrl

    @XmlTransient
    List<Book> books
}
