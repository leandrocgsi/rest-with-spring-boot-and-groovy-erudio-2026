package br.com.erudio.exception.handler

import br.com.erudio.exception.BadRequestException
import br.com.erudio.exception.ExceptionResponse
import br.com.erudio.exception.FileNotFoundException
import br.com.erudio.exception.FileStorageException
import br.com.erudio.exception.InvalidJwtAuthenticationException
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.exception.ResourceNotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
@RestController
class CustomEntityResponseHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail && body.type == null) {
            body.type = URI.create('about:blank')
        }
        super.createResponseEntity(body, headers, statusCode, request)
    }

    @ExceptionHandler(Exception)
    final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(ResourceNotFoundException)
    final ResponseEntity<ExceptionResponse> handleNotFoundExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(RequiredObjectIsNullException)
    final ResponseEntity<ExceptionResponse> handleRequiredObjectExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(BadRequestException)
    final ResponseEntity<ExceptionResponse> handleBadRequestExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(FileNotFoundException)
    final ResponseEntity<ExceptionResponse> handleFileNotFoundExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(FileStorageException)
    final ResponseEntity<ExceptionResponse> handleFileStorageExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(InvalidJwtAuthenticationException)
    final ResponseEntity<ExceptionResponse> handleInvalidJwtAuthenticationExceptions(Exception ex, WebRequest request) {
        respond(ex, request, HttpStatus.FORBIDDEN)
    }

    private static ResponseEntity<ExceptionResponse> respond(Exception ex, WebRequest request, HttpStatus status) {
        new ResponseEntity<>(new ExceptionResponse(new Date(), ex.message, request.getDescription(false)), status)
    }
}
