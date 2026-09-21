package br.com.erudio.integrationtests.dto

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@TupleConstructor
@EqualsAndHashCode
class TokenDTO {

    String username
    Boolean authenticated
    Date created
    Date expiration
    String accessToken
    String refreshToken
}
