package br.com.erudio.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class FileStorageException extends RuntimeException {

    FileStorageException(String message, Throwable cause = null) {
        super(message, cause)
    }
}
