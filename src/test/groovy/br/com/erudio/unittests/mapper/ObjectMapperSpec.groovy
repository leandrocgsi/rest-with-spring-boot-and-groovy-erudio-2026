package br.com.erudio.unittests.mapper

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.model.Person
import br.com.erudio.unittests.mapper.mocks.MockPerson
import spock.lang.Specification

import static br.com.erudio.mapper.ObjectMapper.parseListObjects
import static br.com.erudio.mapper.ObjectMapper.parseObject

class ObjectMapperSpec extends Specification {

    MockPerson input = new MockPerson()

    def 'parses an entity to a DTO'() {
        when:
        def output = parseObject(input.mockEntity(), PersonDTO)

        then:
        with(output) {
            id == 0L
            firstName == 'First Name Test0'
            lastName == 'Last Name Test0'
            address == 'Address Test0'
            gender == 'Male'
        }
    }

    def 'parses a DTO to an entity'() {
        when:
        def output = parseObject(input.mockDTO(), Person)

        then:
        with(output) {
            id == 0L
            firstName == 'First Name Test0'
            lastName == 'Last Name Test0'
            address == 'Address Test0'
            gender == 'Male'
        }
    }

    def 'parses an entity list to a DTO list (item #index)'() {
        when:
        def output = parseListObjects(input.mockEntityList(), PersonDTO)

        then:
        output.size() == 14
        with(output[index]) {
            id == index
            firstName == "First Name Test$index"
            lastName == "Last Name Test$index"
            address == "Address Test$index"
            gender == expectedGender
        }

        where:
        index | expectedGender
        0     | 'Male'
        7     | 'Female'
        12    | 'Male'
    }

    def 'parses a DTO list to an entity list (item #index)'() {
        when:
        def output = parseListObjects(input.mockDTOList(), Person)

        then:
        output.size() == 14
        with(output[index]) {
            id == index
            firstName == "First Name Test$index"
            lastName == "Last Name Test$index"
            address == "Address Test$index"
            gender == expectedGender
        }

        where:
        index | expectedGender
        0     | 'Male'
        7     | 'Female'
        12    | 'Male'
    }

    def 'does not copy the Groovy metaClass between the mapped objects'() {
        when:
        def dto = parseObject(input.mockEntity(), PersonDTO)
        def entity = parseObject(dto, Person)

        then:
        dto.metaClass.theClass == PersonDTO
        entity.metaClass.theClass == Person
    }
}
