package br.com.erudio.file.exporter.contract

import br.com.erudio.data.dto.PersonDTO
import org.springframework.core.io.Resource

interface PersonExporter {

    Resource exportPeople(List<PersonDTO> people)

    Resource exportPerson(PersonDTO person)
}
