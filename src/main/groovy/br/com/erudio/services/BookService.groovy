package br.com.erudio.services

import br.com.erudio.controllers.BookController
import br.com.erudio.data.dto.BookDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.model.Book
import br.com.erudio.repository.BookRepository
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.PagedModel
import org.springframework.stereotype.Service

import static br.com.erudio.mapper.ObjectMapper.parseObject
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

@Slf4j
@Service
@TupleConstructor(includeFields = true, defaults = false)
class BookService {

    private final BookRepository repository
    private final PagedResourcesAssembler<BookDTO> assembler

    PagedModel<EntityModel<BookDTO>> findAll(Pageable pageable) {
        log.info('Finding all Book!')

        Page<BookDTO> booksWithLinks = repository.findAll(pageable).map { toDto(it) }

        Link findAllLink = linkTo(methodOn(BookController)
            .findAll(pageable.pageNumber, pageable.pageSize, pageable.sort.toString()))
            .withSelfRel()
        assembler.toModel(booksWithLinks, findAllLink)
    }

    BookDTO findById(Long id) {
        log.info('Finding one Book!')

        toDto(findEntity(id))
    }

    BookDTO create(BookDTO book) {
        if (book == null) throw new RequiredObjectIsNullException()

        log.info('Creating one Book!')
        toDto(repository.save(parseObject(book, Book)))
    }

    BookDTO update(BookDTO book) {
        if (book == null) throw new RequiredObjectIsNullException()

        log.info('Updating one Book!')
        Book entity = findEntity(book.id)

        entity.author = book.author
        entity.launchDate = book.launchDate
        entity.price = book.price
        entity.title = book.title

        toDto(repository.save(entity))
    }

    void delete(Long id) {
        log.info('Deleting one Book!')

        repository.delete(findEntity(id))
    }

    private Book findEntity(Long id) {
        repository.findById(id).orElseThrow { new ResourceNotFoundException('No records found for this ID!') }
    }

    private static BookDTO toDto(Book entity) {
        BookDTO dto = parseObject(entity, BookDTO)
        addHateoasLinks(dto)
        dto
    }

    private static void addHateoasLinks(BookDTO dto) {
        dto.add(linkTo(methodOn(BookController).findById(dto.id)).withSelfRel().withType('GET'))
        dto.add(linkTo(methodOn(BookController).findAll(1, 12, 'asc')).withRel('findAll').withType('GET'))
        dto.add(linkTo(methodOn(BookController).create(dto)).withRel('create').withType('POST'))
        dto.add(linkTo(methodOn(BookController).update(dto)).withRel('update').withType('PUT'))
        dto.add(linkTo(methodOn(BookController).delete(dto.id)).withRel('delete').withType('DELETE'))
    }
}
