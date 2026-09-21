package br.com.erudio.controllers

import br.com.erudio.controllers.docs.AuthControllerDocs
import br.com.erudio.controllers.mapping.ApiPost
import br.com.erudio.data.dto.security.AccountCredentialsDTO
import br.com.erudio.services.AuthService
import groovy.transform.TupleConstructor
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = 'Authentication Endpoint!')
@RestController
@RequestMapping('/auth')
@TupleConstructor(includeFields = true, defaults = false)
class AuthController implements AuthControllerDocs {

    private final AuthService service

    @PostMapping('/signin')
    @Override
    ResponseEntity<?> signin(@RequestBody AccountCredentialsDTO credentials) {
        if (credentialsIsInvalid(credentials)) return forbidden()

        service.signIn(credentials) ?: forbidden()
    }

    @PutMapping('/refresh/{username}')
    @Override
    ResponseEntity<?> refreshToken(
            @PathVariable('username') String username,
            @RequestHeader('Authorization') String refreshToken) {
        if (!username?.trim() || !refreshToken?.trim()) return forbidden()

        service.refreshToken(username, refreshToken) ?: forbidden()
    }

    @ApiPost('/createUser')
    @Override
    AccountCredentialsDTO create(@RequestBody AccountCredentialsDTO credentials) {
        service.create(credentials)
    }

    private static boolean credentialsIsInvalid(AccountCredentialsDTO credentials) {
        !credentials?.username?.trim() || !credentials?.password?.trim()
    }

    private static ResponseEntity<String> forbidden() {
        ResponseEntity.status(HttpStatus.FORBIDDEN).body('Invalid client request!')
    }
}
