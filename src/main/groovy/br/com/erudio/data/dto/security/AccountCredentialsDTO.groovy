package br.com.erudio.data.dto.security

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@JsonPropertyOrder(['username', 'password', 'fullname'])
@TupleConstructor
@EqualsAndHashCode
class AccountCredentialsDTO implements Serializable {

    String username
    String password
    String fullname
}
