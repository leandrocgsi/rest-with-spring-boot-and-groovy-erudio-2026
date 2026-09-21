package br.com.erudio.unittests.services

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.exception.BadRequestException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.exception.ResourceNotFoundException
import br.com.erudio.file.exporter.contract.PersonExporter
import br.com.erudio.file.exporter.factory.FileExporterFactory
import br.com.erudio.file.importer.contract.FileImporter
import br.com.erudio.file.importer.factory.FileImporterFactory
import br.com.erudio.model.Person
import br.com.erudio.repository.PersonRepository
import br.com.erudio.services.PersonService
import br.com.erudio.unittests.mapper.mocks.MockPerson
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicLong

import static java.nio.charset.StandardCharsets.UTF_8

class PersonServiceFilesSpec extends Specification {

    static final String CSV = 'text/csv'

    MockPerson input = new MockPerson()
    PersonRepository repository = Mock()
    FileImporterFactory importerFactory = Mock()
    FileExporterFactory exporterFactory = Mock()
    PagedResourcesAssembler<PersonDTO> assembler = Mock()
    PersonExporter personExporter = Mock()
    FileImporter fileImporter = Mock()

    def exported = new ByteArrayResource('exported'.getBytes(UTF_8))

    PersonService service = new PersonService(repository, importerFactory, exporterFactory, assembler)

    def 'exportPage exports the requested page in the format of the accept header'() {
        given:
        def pageable = PageRequest.of(0, 3, Sort.by('firstName'))
        repository.findAll(pageable) >> new PageImpl<>(input.mockEntityList().subList(0, 3))
        exporterFactory.getExporter(CSV) >> personExporter

        when:
        def result = service.exportPage(pageable, CSV)

        then:
        1 * personExporter.exportPeople({ List<PersonDTO> people ->
            people*.firstName == ['First Name Test0', 'First Name Test1', 'First Name Test2']
        }) >> exported
        result.is(exported)
    }

    def 'exportPage wraps any failure keeping the cause'() {
        given:
        def pageable = PageRequest.of(0, 3)
        repository.findAll(pageable) >> new PageImpl<>([])
        exporterFactory.getExporter('application/json') >> { throw new BadRequestException('Invalid File Format!') }

        when:
        service.exportPage(pageable, 'application/json')

        then:
        def e = thrown(RuntimeException)
        e.message == 'Error during file export!'
        e.cause instanceof BadRequestException
    }

    def 'exportPerson exports the person that was found'() {
        given:
        repository.findById(1L) >> Optional.of(input.mockEntity(1))
        exporterFactory.getExporter('application/pdf') >> personExporter

        when:
        def result = service.exportPerson(1L, 'application/pdf')

        then:
        1 * personExporter.exportPerson({ PersonDTO person -> person.firstName == 'First Name Test1' }) >> exported
        result.is(exported)
    }

    def 'exportPerson fails when the person does not exist'() {
        given:
        repository.findById(99L) >> Optional.empty()

        when:
        service.exportPerson(99L, 'application/pdf')

        then:
        def e = thrown(ResourceNotFoundException)
        e.message == 'No records found for this ID!'
        0 * exporterFactory._
    }

    def 'exportPerson wraps any failure keeping the cause'() {
        given:
        repository.findById(1L) >> Optional.of(input.mockEntity(1))
        exporterFactory.getExporter('application/pdf') >> personExporter
        personExporter.exportPerson(_ as PersonDTO) >> { throw new IllegalStateException('template broken') }

        when:
        service.exportPerson(1L, 'application/pdf')

        then:
        def e = thrown(RuntimeException)
        e.message == 'Error during file export!'
        e.cause.message == 'template broken'
    }

    def 'massCreation saves every person of the file and returns them with links'() {
        given:
        importerFactory.getImporter('people.csv') >> fileImporter
        fileImporter.importFile(_ as InputStream) >> [dto('Ada'), dto('Alan')]
        def ids = new AtomicLong(100)

        when:
        def created = service.massCreation(csvFile('people.csv', 'irrelevant, the importer is a mock'))

        then:
        2 * repository.save(_ as Person) >> { Person saved ->
            saved.id = ids.incrementAndGet()
            saved
        }
        created*.firstName == ['Ada', 'Alan']
        created*.id == [101L, 102L]
        created.every { !it.links.empty }
    }

    def 'massCreation of an empty file is a bad request'() {
        when:
        service.massCreation(csvFile('people.csv', ''))

        then:
        def e = thrown(BadRequestException)
        e.message == 'Please set a Valid File!'
        0 * importerFactory._
        0 * repository._
    }

    def 'massCreation reports an unsupported file as a storage error'() {
        given:
        importerFactory.getImporter('people.txt') >> { throw new BadRequestException('Invalid File Format!') }

        when:
        service.massCreation(csvFile('people.txt', 'whatever'))

        then:
        def e = thrown(FileStorageException)
        e.message == 'Error processing the file!'
        0 * repository._
    }

    def 'massCreation reports an unreadable file as a storage error'() {
        given:
        importerFactory.getImporter('people.csv') >> fileImporter
        fileImporter.importFile(_ as InputStream) >> { throw new IllegalArgumentException('Mapping for gender not found') }

        when:
        service.massCreation(csvFile('people.csv', 'first_name\nAda\n'))

        then:
        def e = thrown(FileStorageException)
        e.message == 'Error processing the file!'
        0 * repository.save(_)
    }

    def 'massCreation reports a file without a name as a storage error'() {
        given:
        MultipartFile nameless = Mock {
            isEmpty() >> false
            getInputStream() >> InputStream.nullInputStream()
            getOriginalFilename() >> null
        }

        when:
        service.massCreation(nameless)

        then:
        def e = thrown(FileStorageException)
        e.message == 'Error processing the file!'
        0 * importerFactory._
        0 * repository._
    }

    private static PersonDTO dto(String firstName) {
        new PersonDTO(firstName: firstName, lastName: 'Last', address: 'Address', gender: 'Male', enabled: true)
    }

    private static MultipartFile csvFile(String name, String content) {
        new MockMultipartFile('file', name, CSV, content.getBytes(UTF_8))
    }
}
