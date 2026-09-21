package br.com.erudio.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class RequiredObjectIsNullException extends RuntimeException {

    RequiredObjectIsNullException() {
        super('It is not allowed to persist a null object!')
    }

    RequiredObjectIsNullException(String message) {
        super(message)
    }
}
