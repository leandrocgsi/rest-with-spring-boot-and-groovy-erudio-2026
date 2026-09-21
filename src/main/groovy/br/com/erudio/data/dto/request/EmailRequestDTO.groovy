package br.com.erudio.data.dto.request

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class EmailRequestDTO {

    String to
    String subject
    String body
}
