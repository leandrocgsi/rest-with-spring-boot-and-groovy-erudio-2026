package br.com.erudio.unittests.mapper.mocks

import br.com.erudio.data.dto.PersonDTO
import br.com.erudio.model.Person

class MockPerson {

    Person mockEntity(int number = 0) {
        new Person(
            address: "Address Test$number",
            firstName: "First Name Test$number",
            gender: number % 2 == 0 ? 'Male' : 'Female',
            id: number as Long,
            lastName: "Last Name Test$number"
        )
    }

    PersonDTO mockDTO(int number = 0) {
        new PersonDTO(
            address: "Address Test$number",
            firstName: "First Name Test$number",
            gender: number % 2 == 0 ? 'Male' : 'Female',
            id: number as Long,
            lastName: "Last Name Test$number"
        )
    }

    List<Person> mockEntityList() {
        (0..<14).collect { int number -> mockEntity(number) }
    }

    List<PersonDTO> mockDTOList() {
        (0..<14).collect { int number -> mockDTO(number) }
    }
}
