package br.com.erudio.unittests.file

import br.com.erudio.file.importer.impl.CsvImporter
import spock.lang.Specification

import static java.nio.charset.StandardCharsets.UTF_8

class CsvImporterSpec extends Specification {

    CsvImporter importer = new CsvImporter()

    def 'imports one person per line'() {
        when:
        def people = importer.importFile(csv('''\
            first_name,last_name,address,gender
            Ada,Lovelace,London,Female
            Alan,Turing,Wilmslow,Male
            '''.stripIndent()))

        then:
        people.size() == 2
        with(people[0]) {
            firstName == 'Ada'
            lastName == 'Lovelace'
            address == 'London'
            gender == 'Female'
        }
        people[1].firstName == 'Alan'
    }

    def 'imported people are enabled and have no id yet'() {
        when:
        def people = importer.importFile(csv('first_name,last_name,address,gender\nAda,Lovelace,London,Female\n'))

        then:
        people[0].enabled
        people[0].id == null
    }

    def 'the order of the columns does not matter'() {
        when:
        def people = importer.importFile(csv('gender,address,last_name,first_name\nFemale,London,Lovelace,Ada\n'))

        then:
        with(people[0]) {
            firstName == 'Ada'
            lastName == 'Lovelace'
            address == 'London'
            gender == 'Female'
        }
    }

    def 'trims spaces around the values'() {
        when:
        def people = importer.importFile(csv('first_name,last_name,address,gender\n  Ada  ,  Lovelace ,  London  , Female \n'))

        then:
        with(people[0]) {
            firstName == 'Ada'
            lastName == 'Lovelace'
            address == 'London'
            gender == 'Female'
        }
    }

    def 'ignores empty lines'() {
        when:
        def people = importer.importFile(csv('first_name,last_name,address,gender\n\nAda,Lovelace,London,Female\n\n\nAlan,Turing,Wilmslow,Male\n\n'))

        then:
        people.size() == 2
    }

    def 'keeps commas inside quoted values'() {
        when:
        def people = importer.importFile(csv('first_name,last_name,address,gender\nAda,Lovelace,"Rua A, 10 - apto 2",Female\n'))

        then:
        people[0].address == 'Rua A, 10 - apto 2'
    }

    def 'reads UTF-8 accents'() {
        when:
        def people = importer.importFile(csv('first_name,last_name,address,gender\nJoão,Conceição,Avenida São João,Male\n'))

        then:
        with(people[0]) {
            firstName == 'João'
            lastName == 'Conceição'
            address == 'Avenida São João'
        }
    }

    def 'imports nothing from #description'() {
        expect:
        importer.importFile(csv(content)).empty

        where:
        description         | content
        'a header-only file' | 'first_name,last_name,address,gender\n'
        'an empty file'      | ''
    }

    def 'fails when a required column is missing'() {
        when:
        importer.importFile(csv('first_name,last_name,address\nAda,Lovelace,London\n'))

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('gender')
    }

    def 'fails when a line has fewer values than the header'() {
        when:
        importer.importFile(csv('first_name,last_name,address,gender\nAda,Lovelace\n'))

        then:
        thrown(IllegalArgumentException)
    }

    private static InputStream csv(String content) {
        new ByteArrayInputStream(content.getBytes(UTF_8))
    }
}
