package br.com.erudio.unittests.services

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.model.Person
import br.com.erudio.repository.PersonRepository
import br.com.erudio.services.PersonService
import br.com.erudio.unittests.mapper.mocks.MockPerson
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

class PersonServiceSpec extends Specification {

    MockPerson input = new MockPerson()
    PersonRepository repository = Mock()
    FileImporterFactory importerFactory = Mock()
    FileExporterFactory exporterFactory = Mock()
    PagedResourcesAssembler<PersonDTO> assembler = Mock()

    PersonService service = new PersonService(repository, importerFactory, exporterFactory, assembler)

    def 'finds a person by id with its HATEOAS links'() {
        given:
        repository.findById(1L) >> Optional.of(input.mockEntity(1))

        when:
        def result = service.findById(1L)

        then:
        result.id == 1L
        result.address == 'Address Test1'
        result.firstName == 'First Name Test1'
        result.lastName == 'Last Name Test1'
        result.gender == 'Female'
        hasPersonLinks(result, 1)
    }

    def 'fails to find a person that does not exist'() {
        given:
        repository.findById(99L) >> Optional.empty()

        when:
        service.findById(99L)

        then:
        def e = thrown(ResourceNotFoundException)
        e.message == 'No records found for this ID!'
    }

    def 'creates a person'() {
        given:
        repository.save(_ as Person) >> input.mockEntity(1)

        when:
        def result = service.create(input.mockDTO(1))

        then:
        result.id == 1L
        result.address == 'Address Test1'
        result.firstName == 'First Name Test1'
        result.lastName == 'Last Name Test1'
        result.gender == 'Female'
        hasPersonLinks(result, 1)
    }

    def 'creating a null person is rejected'() {
        when:
        service.create(null)

        then:
        def e = thrown(RequiredObjectIsNullException)
        e.message.contains('It is not allowed to persist a null object!')
    }

    def 'updates a person'() {
        given:
        def person = input.mockEntity(1)
        repository.findById(1L) >> Optional.of(person)
        repository.save(person) >> person

        when:
        def result = service.update(input.mockDTO(1))

        then:
        result.id == 1L
        result.address == 'Address Test1'
        result.firstName == 'First Name Test1'
        result.lastName == 'Last Name Test1'
        result.gender == 'Female'
        hasPersonLinks(result, 1)
    }

    def 'updating a null person is rejected'() {
        when:
        service.update(null)

        then:
        def e = thrown(RequiredObjectIsNullException)
        e.message.contains('It is not allowed to persist a null object!')
    }

    def 'disables a person and returns it with its links'() {
        given:
        def enabled = input.mockEntity(1)
        enabled.enabled = true
        def disabled = input.mockEntity(1)
        disabled.enabled = false

        when:
        def result = service.disablePerson(1L)

        then:
        2 * repository.findById(1L) >>> [Optional.of(enabled), Optional.of(disabled)]
        1 * repository.disablePerson(1L)
        result.enabled == false
        hasPersonLinks(result, 1)
    }

    def 'disabling a person that does not exist fails without touching the database'() {
        given:
        repository.findById(99L) >> Optional.empty()

        when:
        service.disablePerson(99L)

        then:
        thrown(ResourceNotFoundException)
        0 * repository.disablePerson(_)
    }

    def 'deletes a person'() {
        given:
        def person = input.mockEntity(1)

        when:
        service.delete(1L)

        then:
        1 * repository.findById(1L) >> Optional.of(person)
        1 * repository.delete(_ as Person)
        0 * _
    }

    def 'finds all people and gives the assembler the DTOs with their links'() {
        given:
        repository.findAll(_ as Pageable) >> new PageImpl<>(input.mockEntityList())
        Link selfLink = null
        assembler.toModel(_ as Page, _ as Link) >> { Page<PersonDTO> dtos, Link link ->
            selfLink = link
            PagedModel.of(
                dtos.content.collect { EntityModel.of(it) },
                new PagedModel.PageMetadata(dtos.size, dtos.number, dtos.totalElements, dtos.totalPages))
        }

        when:
        def result = service.findAll(PageRequest.of(0, 14))
        def people = result.content*.content

        then:
        people.size() == 14
        [1, 4, 7].every { int index ->
            def person = people[index]
            person.address == "Address Test$index" &&
                person.firstName == "First Name Test$index" &&
                person.lastName == "Last Name Test$index" &&
                person.gender == (index % 2 == 0 ? 'Male' : 'Female') &&
                hasPersonLinks(person, index)
        }
        selfLink.rel.value() == 'self'
        selfLink.href.contains('/api/person/v1')
    }

    def 'finds people by name'() {
        given:
        def pageable = PageRequest.of(0, 12)
        1 * repository.findPeopleByName('iko', pageable) >> new PageImpl<>([input.mockEntity(1)])
        assembler.toModel(_ as Page, _ as Link) >> { Page<PersonDTO> dtos, Link link ->
            PagedModel.of(
                dtos.content.collect { EntityModel.of(it) },
                new PagedModel.PageMetadata(dtos.size, dtos.number, dtos.totalElements, dtos.totalPages))
        }

        when:
        def result = service.findByName('iko', pageable)

        then:
        result.content.size() == 1
        result.content.first().content.firstName == 'First Name Test1'
    }

    private static boolean hasPersonLinks(PersonDTO person, long id) {
        def links = linksOf(person)
        links.keySet() == ['findAll', 'findByName', 'self', 'create', 'massCreation', 'update', 'disable', 'delete', 'exportPage'] as Set &&
            links.self.href.endsWith("/api/person/v1/$id") && links.self.type == 'GET' &&
            links.findAll.href.contains('/api/person/v1?') && links.findAll.type == 'GET' &&
            links.findByName.href.contains('/api/person/v1/findPeopleByName/') && links.findByName.type == 'GET' &&
            links.create.href.endsWith('/api/person/v1') && links.create.type == 'POST' &&
            links.massCreation.href.endsWith('/api/person/v1/massCreation') && links.massCreation.type == 'POST' &&
            links.update.href.endsWith('/api/person/v1') && links.update.type == 'PUT' &&
            links.disable.href.endsWith("/api/person/v1/$id") && links.disable.type == 'PATCH' &&
            links.delete.href.endsWith("/api/person/v1/$id") && links.delete.type == 'DELETE' &&
            links.exportPage.href.contains('/api/person/v1/exportPage?') && links.exportPage.type == 'GET'
    }
}
