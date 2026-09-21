package br.com.erudio.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException extends RuntimeException {

    BadRequestException() {
        super('Unsupported file extension!')
    }

    BadRequestException(String message) {
        super(message)
    }
}
