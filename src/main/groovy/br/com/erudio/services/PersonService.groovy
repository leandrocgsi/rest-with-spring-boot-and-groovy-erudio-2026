package br.com.erudio.services

import br.com.erudio.controllers.PersonController
import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.exception.BadRequestException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.model.Person
import br.com.erudio.repository.PersonRepository
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import jakarta.transaction.Transactional
import org.springframework.core.io.Resource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.PagedModel
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

import static br.com.erudio.mapper.ObjectMapper.parseObject
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn

@Slf4j
@Service
@TupleConstructor(includeFields = true, defaults = false)
class PersonService {

    private final PersonRepository repository
    private final FileImporterFactory importer
    private final FileExporterFactory exporter
    private final PagedResourcesAssembler<PersonDTO> assembler

    PagedModel<EntityModel<PersonDTO>> findAll(Pageable pageable) {
        log.info('Finding all People!')

        buildPagedModel(pageable, repository.findAll(pageable))
    }

    PagedModel<EntityModel<PersonDTO>> findByName(String firstName, Pageable pageable) {
        log.info('Finding People by name!')

        buildPagedModel(pageable, repository.findPeopleByName(firstName, pageable))
    }

    Resource exportPage(Pageable pageable, String acceptHeader) {
        log.info('Exporting a People page!')

        List<PersonDTO> people = repository.findAll(pageable).content
            .collect { Person person -> parseObject(person, PersonDTO) }

        try {
            exporter.getExporter(acceptHeader).exportPeople(people)
        } catch (Exception e) {
            throw new RuntimeException('Error during file export!', e)
        }
    }

    Resource exportPerson(Long id, String acceptHeader) {
        log.info('Exporting data of one Person!')

        PersonDTO person = parseObject(findEntity(id), PersonDTO)

        try {
            exporter.getExporter(acceptHeader).exportPerson(person)
        } catch (Exception e) {
            throw new RuntimeException('Error during file export!', e)
        }
    }

    PersonDTO findById(Long id) {
        log.info('Finding one Person!')

        toDto(findEntity(id))
    }

    PersonDTO create(PersonDTO person) {
        if (person == null) throw new RequiredObjectIsNullException()

        log.info('Creating one Person!')
        toDto(repository.save(parseObject(person, Person)))
    }

    List<PersonDTO> massCreation(MultipartFile file) {
        log.info('Importing People from file!')

        if (file.empty) throw new BadRequestException('Please set a Valid File!')

        try {
            file.inputStream.withCloseable { InputStream inputStream ->
                String filename = file.originalFilename
                if (filename == null) throw new BadRequestException('File name cannot be null')

                List<Person> entities = importer.getImporter(filename).importFile(inputStream)
                    .collect { PersonDTO dto -> repository.save(parseObject(dto, Person)) }

                entities.collect { Person entity -> toDto(entity) }
            }
        } catch (Exception ignored) {
            throw new FileStorageException('Error processing the file!')
        }
    }

    PersonDTO update(PersonDTO person) {
        if (person == null) throw new RequiredObjectIsNullException()

        log.info('Updating one Person!')
        Person entity = findEntity(person.id)

        entity.firstName = person.firstName
        entity.lastName = person.lastName
        entity.address = person.address
        entity.gender = person.gender

        toDto(repository.save(entity))
    }

    @Transactional
    PersonDTO disablePerson(Long id) {
        log.info('Disabling one Person!')

        findEntity(id)
        repository.disablePerson(id)

        toDto(findEntity(id))
    }

    void delete(Long id) {
        log.info('Deleting one Person!')

        repository.delete(findEntity(id))
    }

    private Person findEntity(Long id) {
        repository.findById(id).orElseThrow { new ResourceNotFoundException('No records found for this ID!') }
    }

    private PagedModel<EntityModel<PersonDTO>> buildPagedModel(Pageable pageable, Page<Person> people) {
        Page<PersonDTO> peopleWithLinks = people.map { Person person -> toDto(person) }

        Link findAllLink = linkTo(methodOn(PersonController)
            .findAll(pageable.pageNumber, pageable.pageSize, String.valueOf(pageable.sort)))
            .withSelfRel()
        assembler.toModel(peopleWithLinks, findAllLink)
    }

    private static PersonDTO toDto(Person entity) {
        PersonDTO dto = parseObject(entity, PersonDTO)
        addHateoasLinks(dto)
        dto
    }

    private static void addHateoasLinks(PersonDTO dto) {
        dto.add(linkTo(methodOn(PersonController).findAll(1, 12, 'asc')).withRel('findAll').withType('GET'))
        dto.add(linkTo(methodOn(PersonController).findByName('', 1, 12, 'asc')).withRel('findByName').withType('GET'))
        dto.add(linkTo(methodOn(PersonController).findById(dto.id)).withSelfRel().withType('GET'))
        dto.add(linkTo(methodOn(PersonController).create(dto)).withRel('create').withType('POST'))
        dto.add(linkTo(methodOn(PersonController)).slash('massCreation').withRel('massCreation').withType('POST'))
        dto.add(linkTo(methodOn(PersonController).update(dto)).withRel('update').withType('PUT'))
        dto.add(linkTo(methodOn(PersonController).disablePerson(dto.id)).withRel('disable').withType('PATCH'))
        dto.add(linkTo(methodOn(PersonController).delete(dto.id)).withRel('delete').withType('DELETE'))

        dto.add(linkTo(methodOn(PersonController).exportPage(1, 12, 'asc', null))
            .withRel('exportPage')
            .withType('GET')
            .withTitle('Export People'))
    }
}
