package br.com.erudio.exception

import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
class InvalidJwtAuthenticationException extends AuthenticationException {

    InvalidJwtAuthenticationException(String message) {
        super(message)
    }
}
