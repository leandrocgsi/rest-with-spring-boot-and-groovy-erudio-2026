package br.com.erudio.data.dto.security

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@JsonPropertyOrder(['username', 'authenticated', 'created', 'expiration', 'accessToken', 'refreshToken'])
@TupleConstructor
@EqualsAndHashCode
class TokenDTO implements Serializable {

    String username
    Boolean authenticated
    Date created
    Date expiration
    String accessToken
    String refreshToken
}
