package br.com.erudio.unittests.services

import br.com.erudio.data.dto.BookDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.model.Book
import br.com.erudio.repository.BookRepository
import br.com.erudio.services.BookService
import br.com.erudio.unittests.mapper.mocks.MockBook
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.PagedModel
import spock.lang.Specification

import static br.com.erudio.testsupport.HateoasSupport.linksOf

class BookServiceSpec extends Specification {

    MockBook input = new MockBook()
    BookRepository repository = Mock()
    PagedResourcesAssembler<BookDTO> assembler = Mock()

    BookService service = new BookService(repository, assembler)

    def 'finds a book by id with its HATEOAS links'() {
        given:
        repository.findById(1L) >> Optional.of(input.mockEntity(1))

        when:
        def result = service.findById(1L)

        then:
        result.id == 1L
        result.author == 'Some Author1'
        result.price == 25D
        result.title == 'Some Title1'
        result.launchDate != null
        hasBookLinks(result, 1)
    }

    def 'fails to find a book that does not exist'() {
        given:
        repository.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        def e = thrown(ResourceNotFoundException)
        e.message == 'No records found for this ID!'
    }

    def 'creates a book'() {
        given:
        repository.save(_ as Book) >> input.mockEntity(1)

        when:
        def result = service.create(input.mockDTO(1))

        then:
        result.id == 1L
        result.author == 'Some Author1'
        result.price == 25D
        result.title == 'Some Title1'
        result.launchDate != null
        hasBookLinks(result, 1)
    }

    def 'creating a null book is rejected'() {
        when:
        service.create(null)

        then:
        def e = thrown(RequiredObjectIsNullException)
        e.message.contains('It is not allowed to persist a null object!')
    }

    def 'updates a book'() {
        given:
        def book = input.mockEntity(1)
        repository.findById(1L) >> Optional.of(book)
        repository.save(book) >> book

        when:
        def result = service.update(input.mockDTO(1))

        then:
        result.id == 1L
        result.author == 'Some Author1'
        result.price == 25D
        result.title == 'Some Title1'
        result.launchDate != null
        hasBookLinks(result, 1)
    }

    def 'updating a null book is rejected'() {
        when:
        service.update(null)

        then:
        def e = thrown(RequiredObjectIsNullException)
        e.message.contains('It is not allowed to persist a null object!')
    }

    def 'deletes a book'() {
        given:
        def book = input.mockEntity(1)

        when:
        service.delete(1L)

        then:
        1 * repository.findById(1L) >> Optional.of(book)
        1 * repository.delete(_ as Book)
        0 * _
    }

    def 'finds all books and gives the assembler the DTOs with their links'() {
        given:
        def page = new PageImpl<>(input.mockEntityList())
        repository.findAll(_ as Pageable) >> page

        Link selfLink = null
        assembler.toModel(_ as Page, _ as Link) >> { Page<BookDTO> dtos, Link link ->
            selfLink = link
            PagedModel.of(
                dtos.content.collect { EntityModel.of(it) },
                new PagedModel.PageMetadata(dtos.size, dtos.number, dtos.totalElements, dtos.totalPages))
        }

        when:
        def result = service.findAll(PageRequest.of(0, 14))
        def books = result.content*.content

        then:
        books.size() == 14
        [1, 4, 7].every { int index ->
            def book = books[index]
            book.author == "Some Author$index" && book.price == 25D && book.title == "Some Title$index" && hasBookLinks(book, index)
        }
        selfLink.rel.value() == 'self'
        selfLink.href.contains('/api/book/v1')
    }

    private static boolean hasBookLinks(BookDTO book, long id) {
        def links = linksOf(book)
        links.keySet() == ['self', 'findAll', 'create', 'update', 'delete'] as Set &&
            links.self.href.endsWith("/api/book/v1/$id") && links.self.type == 'GET' &&
            links.findAll.href.contains('/api/book/v1?') && links.findAll.type == 'GET' &&
            links.create.href.endsWith('/api/book/v1') && links.create.type == 'POST' &&
            links.update.href.endsWith('/api/book/v1') && links.update.type == 'PUT' &&
            links.delete.href.endsWith("/api/book/v1/$id") && links.delete.type == 'DELETE'
    }
}
