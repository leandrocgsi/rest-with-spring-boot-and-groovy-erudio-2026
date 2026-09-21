package br.com.erudio.integrationtests.dto.wrappers.xmlandyaml

import br.com.erudio.integrationtests.dto.BookDTO
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
class PagedModelBook {

    @XmlElement(name = 'content')
    List<BookDTO> content
}
