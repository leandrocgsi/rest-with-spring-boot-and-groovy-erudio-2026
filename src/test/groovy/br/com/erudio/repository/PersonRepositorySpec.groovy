package br.com.erudio.repository

import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationSpec
import br.com.erudio.model.Person
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersonRepositorySpec extends AbstractIntegrationSpec {

    @Autowired
    PersonRepository repository

    @Shared
    Person person = new Person()

    def 'find people by name'() {
        given:
        def pageable = PageRequest.of(0, 12, Sort.by(Sort.Direction.ASC, 'firstName'))

        when:
        person = repository.findPeopleByName('iko', pageable).content.first()

        then:
        with(person) {
            id != null
            firstName == 'Nikola'
            lastName == 'Tesla'
            gender == 'Male'
            enabled
        }
    }

    def 'disable a person'() {
        given:
        def personId = person.id

        when:
        repository.disablePerson(personId)
        person = repository.findById(personId).get()

        then:
        with(person) {
            id != null
            firstName == 'Nikola'
            lastName == 'Tesla'
            gender == 'Male'
            !enabled
        }
    }
}
