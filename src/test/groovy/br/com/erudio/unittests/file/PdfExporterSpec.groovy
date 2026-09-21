package br.com.erudio.unittests.file

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.file.exporter.impl.PdfExporter
import br.com.erudio.model.Book
import br.com.erudio.services.QRCodeService
import br.com.erudio.testsupport.NetworkAssumptions
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.springframework.core.io.Resource
import spock.lang.Requires
import spock.lang.Specification

import java.time.LocalDate
import java.util.concurrent.Callable
import java.util.concurrent.Executors

import static java.nio.charset.StandardCharsets.US_ASCII

@Requires({ NetworkAssumptions.reportImagesAreReachable() })
class PdfExporterSpec extends Specification {

    PdfExporter exporter = new PdfExporter(new QRCodeService())

    def 'exportPeople generates a PDF with the people'() {
        when:
        def resource = exporter.exportPeople([person(1L, 'Ayrton', 'Senna'), person(2L, 'Marie', 'Curie')])
        def pdf = read(resource)

        then:
        new String(resource.contentAsByteArray, 0, 4, US_ASCII) == '%PDF'
        pdf.pages == 1
        pdf.text.contains('PEOPLE REPORT')
        pdf.text.contains('Ayrton')
        pdf.text.contains('Marie')
    }

    def 'exportPeople shows the name of the course in the page header'() {
        when:
        def pdf = read(exporter.exportPeople([person(1L, 'Ayrton', 'Senna')]))

        then:
        pdf.text.contains('Spring Boot 2026')
        !pdf.text.contains('RESTful from 0')
    }

    def 'exportPeople spans several pages when there are many people'() {
        given:
        def people = (1L..120L).collect { Long index -> person(index, "Person$index", 'Number') }

        when:
        def pdf = read(exporter.exportPeople(people))

        then:
        pdf.pages > 1
        pdf.text.contains('Person1 ')
        pdf.text.contains('Person120')
    }

    def 'exportPeople generates a valid PDF even without people'() {
        when:
        def resource = exporter.exportPeople([])
        def pdf = read(resource)

        then:
        new String(resource.contentAsByteArray, 0, 4, US_ASCII) == '%PDF'
        pdf.pages == 1
        !pdf.text.contains('Ayrton')
    }

    def 'exportPeople gives the same result when the compiled report is reused'() {
        given:
        def people = [person(1L, 'Ayrton', 'Senna')]

        when:
        def first = read(exporter.exportPeople(people))
        def second = read(exporter.exportPeople(people))

        then:
        first.pages == second.pages
        second.text.contains('Ayrton')
    }

    def 'exportPeople is safe to call from several threads at the same time'() {
        given:
        def people = [person(1L, 'Ayrton', 'Senna')]

        when:
        def texts = Executors.newFixedThreadPool(8).withCloseable { pool ->
            (1..16)
                .collect { pool.submit({ read(exporter.exportPeople(people)) } as Callable) }
                .collect { it.get().text }
        }

        then:
        texts.size() == 16
        texts.every { it.contains('Ayrton') }
    }

    def 'exportPerson gives the same result when the compiled reports are reused'() {
        given:
        def person = person(1L, 'Ayrton', 'Senna')
        person.profileUrl = 'https://en.wikipedia.org/wiki/Ayrton_Senna'
        person.books = [book(1L, 'Clean Code', 'Robert C. Martin')]

        when:
        def first = read(exporter.exportPerson(person))
        def second = read(exporter.exportPerson(person))

        then:
        first.pages == second.pages
        second.text.contains('Clean Code')
    }

    def 'exportPerson generates a PDF with the person and their books'() {
        given:
        def person = person(1L, 'Ayrton', 'Senna')
        person.profileUrl = 'https://en.wikipedia.org/wiki/Ayrton_Senna'
        person.photoUrl = 'https://raw.githubusercontent.com/leandrocgsi/rest-with-spring-boot-and-java-erudio/refs/heads/main/photos/01_senna.jpg'
        person.books = [
            book(1L, 'Working effectively with legacy code', 'Michael C. Feathers'),
            book(2L, 'Clean Code', 'Robert C. Martin')
        ]

        when:
        def resource = exporter.exportPerson(person)
        def pdf = read(resource)

        then:
        new String(resource.contentAsByteArray, 0, 4, US_ASCII) == '%PDF'
        pdf.text.contains('Ayrton')
        pdf.text.contains('Clean Code')
        pdf.text.contains('Working effectively')
    }

    private static PersonDTO person(Long id, String firstName, String lastName) {
        new PersonDTO(
            id: id,
            firstName: firstName,
            lastName: lastName,
            address: "Address of $firstName",
            gender: 'Male',
            enabled: true
        )
    }

    private static Book book(Long id, String title, String author) {
        new Book(id: id, title: title, author: author, price: 49.9, launchDate: LocalDate.of(2017, 11, 29))
    }

    private static Map read(Resource pdf) {
        def reader = new PdfReader(pdf.contentAsByteArray)
        try {
            def extractor = new PdfTextExtractor(reader)
            def text = (1..reader.numberOfPages).collect { int page -> extractor.getTextFromPage(page) + '\n' }.join()
            [pages: reader.numberOfPages, text: text]
        } finally {
            reader.close()
        }
    }
}
