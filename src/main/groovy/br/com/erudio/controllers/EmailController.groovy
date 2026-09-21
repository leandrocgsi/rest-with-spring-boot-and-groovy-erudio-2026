package br.com.erudio.controllers

import br.com.erudio.controllers.docs.EmailControllerDocs
import br.com.erudio.data.dto.request.EmailRequestDTO
import br.com.erudio.services.EmailService
import groovy.transform.TupleConstructor
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping('/api/email/v1')
@TupleConstructor(includeFields = true, defaults = false)
class EmailController implements EmailControllerDocs {

    private final EmailService service

    @PostMapping
    @Override
    ResponseEntity<String> sendEmail(@RequestBody EmailRequestDTO emailRequest) {
        service.sendSimpleEmail(emailRequest)
        new ResponseEntity<>('e-Mail sent with success!', HttpStatus.OK)
    }

    @PostMapping(value = '/withAttachment', consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    ResponseEntity<String> sendEmailWithAttachment(
            @RequestParam('emailRequest') String emailRequest,
            @RequestParam('attachment') MultipartFile attachment) {
        service.setEmailWithAttachment(emailRequest, attachment)
        new ResponseEntity<>('e-Mail with attachment sent successfully!', HttpStatus.OK)
    }
}
