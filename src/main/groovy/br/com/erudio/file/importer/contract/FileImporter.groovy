package br.com.erudio.file.importer.contract

import br.com.erudio.data.dto.PersonDTO

interface FileImporter {

    List<PersonDTO> importFile(InputStream inputStream)
}
