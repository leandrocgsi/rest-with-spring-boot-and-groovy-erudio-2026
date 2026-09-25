package br.com.erudio.unittests.dto

import br.com.erudio.data.dto.PersonDTO
import spock.lang.Specification

class PersonDTOSpec extends Specification {

    def 'joins the first and the last name'() {
        expect:
        new PersonDTO(firstName: firstName, lastName: lastName).name == expected

        where:
        firstName | lastName || expected
        'Ada'     | 'Lovelace' || 'Ada Lovelace'
        'Ada'     | null       || 'Ada'
        null      | 'Lovelace' || 'Lovelace'
        ''        | 'Lovelace' || 'Lovelace'
        'Ada'     | ''         || 'Ada'
        null      | null       || ''
    }
}
