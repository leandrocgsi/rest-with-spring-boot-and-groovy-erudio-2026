package br.com.erudio.controllers

import br.com.erudio.controllers.docs.PersonControllerDocs
import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.MediaTypes
import br.com.erudio.services.PersonService
import groovy.transform.TupleConstructor
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.Resource
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE
import static org.springframework.http.MediaType.APPLICATION_YAML_VALUE

@RestController
@RequestMapping('/api/person/v1')
@Tag(name = 'People', description = 'Endpoints for Managing People')
@TupleConstructor(includeFields = true, defaults = false)
class PersonController implements PersonControllerDocs {

    private static final Map<String, String> FILE_EXTENSIONS = [
        (MediaTypes.APPLICATION_XLSX_VALUE): '.xlsx',
        (MediaTypes.APPLICATION_CSV_VALUE) : '.csv',
        (MediaTypes.APPLICATION_PDF_VALUE) : '.pdf'
    ].asImmutable()

    private final PersonService service

    @GetMapping(produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    ResponseEntity<PagedModel<EntityModel<PersonDTO>>> findAll(
            @RequestParam(value = 'page', defaultValue = '0') Integer page,
            @RequestParam(value = 'size', defaultValue = '12') Integer size,
            @RequestParam(value = 'direction', defaultValue = 'asc') String direction) {
        ResponseEntity.ok(service.findAll(pageableOf(page, size, direction)))
    }

    @GetMapping(value = '/exportPage',
        produces = [MediaTypes.APPLICATION_XLSX_VALUE, MediaTypes.APPLICATION_CSV_VALUE, MediaTypes.APPLICATION_PDF_VALUE])
    @Override
    ResponseEntity<Resource> exportPage(
            @RequestParam(value = 'page', defaultValue = '0') Integer page,
            @RequestParam(value = 'size', defaultValue = '12') Integer size,
            @RequestParam(value = 'direction', defaultValue = 'asc') String direction,
            HttpServletRequest request) {
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT)

        Resource file = service.exportPage(pageableOf(page, size, direction), acceptHeader)

        String fileExtension = FILE_EXTENSIONS.getOrDefault(acceptHeader, '')
        String contentType = acceptHeader ?: 'application/octet-stream'
        String contentDisposition = "attachment; filename=\"people_exported$fileExtension\""

        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
            .body(file)
    }

    @GetMapping(value = '/findPeopleByName/{firstName}',
        produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    ResponseEntity<PagedModel<EntityModel<PersonDTO>>> findByName(
            @PathVariable('firstName') String firstName,
            @RequestParam(value = 'page', defaultValue = '0') Integer page,
            @RequestParam(value = 'size', defaultValue = '12') Integer size,
            @RequestParam(value = 'direction', defaultValue = 'asc') String direction) {
        ResponseEntity.ok(service.findByName(firstName, pageableOf(page, size, direction)))
    }

    @GetMapping(value = '/{id}',
        produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    PersonDTO findById(@PathVariable('id') Long id) {
        service.findById(id)
    }

    @GetMapping(value = '/export/{id}', produces = [MediaTypes.APPLICATION_PDF_VALUE])
    @Override
    ResponseEntity<Resource> export(@PathVariable('id') Long id, HttpServletRequest request) {
        String acceptHeader = request.getHeader(HttpHeaders.ACCEPT)
        Resource file = service.exportPerson(id, acceptHeader)

        ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(acceptHeader))
            .header(HttpHeaders.CONTENT_DISPOSITION, 'attachment; filename=person.pdf')
            .body(file)
    }

    @PostMapping(
        consumes = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE],
        produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    PersonDTO create(@RequestBody PersonDTO person) {
        service.create(person)
    }

    @PostMapping(value = '/massCreation',
        produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    List<PersonDTO> massCreation(@RequestParam('file') MultipartFile file) {
        service.massCreation(file)
    }

    @PutMapping(
        consumes = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE],
        produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    PersonDTO update(@RequestBody PersonDTO person) {
        service.update(person)
    }

    @PatchMapping(value = '/{id}',
        produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE, APPLICATION_YAML_VALUE])
    @Override
    PersonDTO disablePerson(@PathVariable('id') Long id) {
        service.disablePerson(id)
    }

    @DeleteMapping('/{id}')
    @Override
    ResponseEntity<?> delete(@PathVariable('id') Long id) {
        service.delete(id)
        ResponseEntity.noContent().build()
    }

    private static Pageable pageableOf(Integer page, Integer size, String direction) {
        Sort.Direction sortDirection = 'desc'.equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC
        PageRequest.of(page, size, Sort.by(sortDirection, 'firstName'))
    }
}
