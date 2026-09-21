package br.com.erudio.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class FileNotFoundException extends RuntimeException {

    FileNotFoundException(String message, Throwable cause = null) {
        super(message, cause)
    }
}
