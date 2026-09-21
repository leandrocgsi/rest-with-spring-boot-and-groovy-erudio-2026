package br.com.erudio.controllers

import br.com.erudio.controllers.docs.BookControllerDocs
import br.com.erudio.controllers.mapping.ApiGet
import br.com.erudio.controllers.mapping.ApiPost
import br.com.erudio.controllers.mapping.ApiPut
import br.com.erudio.data.dto.BookDTO
import br.com.erudio.services.BookService
import groovy.transform.TupleConstructor
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping('/api/book/v1')
@Tag(name = 'Book', description = 'Endpoints for Managing Book')
@TupleConstructor(includeFields = true, defaults = false)
class BookController implements BookControllerDocs {

    private final BookService service

    @ApiGet
    @Override
    ResponseEntity<PagedModel<EntityModel<BookDTO>>> findAll(
            @RequestParam(value = 'page', defaultValue = '0') Integer page,
            @RequestParam(value = 'size', defaultValue = '12') Integer size,
            @RequestParam(value = 'direction', defaultValue = 'asc') String direction) {
        ResponseEntity.ok(service.findAll(pageableOf(page, size, direction)))
    }

    @ApiGet('/{id}')
    @Override
    BookDTO findById(@PathVariable('id') Long id) {
        service.findById(id)
    }

    @ApiPost
    @Override
    BookDTO create(@RequestBody BookDTO book) {
        service.create(book)
    }

    @ApiPut
    @Override
    BookDTO update(@RequestBody BookDTO book) {
        service.update(book)
    }

    @DeleteMapping('/{id}')
    @Override
    ResponseEntity<?> delete(@PathVariable('id') Long id) {
        service.delete(id)
        ResponseEntity.noContent().build()
    }

    private static Pageable pageableOf(Integer page, Integer size, String direction) {
        Sort.Direction sortDirection = 'desc'.equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC
        PageRequest.of(page, size, Sort.by(sortDirection, 'title'))
    }
}
