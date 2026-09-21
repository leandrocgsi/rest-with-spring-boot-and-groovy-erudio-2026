package br.com.erudio.integrationtests.dto

import groovy.transform.EqualsAndHashCode
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlRootElement
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter

import java.time.LocalDate

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode
class BookDTO implements Serializable {

    Long id
    String author

    @XmlJavaTypeAdapter(LocalDateXmlAdapter)
    LocalDate launchDate

    Double price
    String title
}
