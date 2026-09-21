package br.com.erudio.mail

import groovy.transform.Immutable

@Immutable(knownImmutableClasses = [File])
class EmailMessage {

    String to
    String subject
    String body
    File attachment
    String attachmentName
}
