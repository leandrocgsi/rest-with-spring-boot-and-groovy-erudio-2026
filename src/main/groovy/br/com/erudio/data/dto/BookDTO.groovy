package br.com.erudio.data.dto

import groovy.transform.EqualsAndHashCode
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

import java.time.LocalDate

@Relation(collectionRelation = 'books')
@EqualsAndHashCode(callSuper = false)
class BookDTO extends RepresentationModel<BookDTO> implements Serializable {

    Long id
    String author
    LocalDate launchDate
    Double price
    String title
}
