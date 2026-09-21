package br.com.erudio.exception.handler

import br.com.erudio.exception.ExceptionResponse
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
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
        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus)
        HttpStatus status = responseStatus?.code() ?: HttpStatus.INTERNAL_SERVER_ERROR

        ResponseEntity.status(status).body(new ExceptionResponse(new Date(), ex.message, request.getDescription(false)))
    }
}
