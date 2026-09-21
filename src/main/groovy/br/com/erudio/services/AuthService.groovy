package br.com.erudio.services

import br.com.erudio.data.dto.security.AccountCredentialsDTO
import br.com.erudio.data.dto.security.TokenDTO
import br.com.erudio.exception.RequiredObjectIsNullException
import br.com.erudio.model.User
import br.com.erudio.repository.UserRepository
import br.com.erudio.security.jwt.JwtTokenProvider
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Slf4j
@Service
@TupleConstructor(includeFields = true, defaults = false)
class AuthService {

    private final AuthenticationManager authenticationManager
    private final JwtTokenProvider tokenProvider
    private final UserRepository repository
    private final PasswordEncoder passwordEncoder

    ResponseEntity<TokenDTO> signIn(AccountCredentialsDTO credentials) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(credentials.username, credentials.password)
        )

        User user = repository.findByUsername(credentials.username)
        if (user == null) {
            throw new UsernameNotFoundException("Username $credentials.username not found!")
        }

        ResponseEntity.ok(tokenProvider.createAccessToken(credentials.username, user.roles))
    }

    ResponseEntity<TokenDTO> refreshToken(String username, String refreshToken) {
        if (repository.findByUsername(username) == null) {
            throw new UsernameNotFoundException("Username $username not found!")
        }

        ResponseEntity.ok(tokenProvider.refreshToken(refreshToken))
    }

    AccountCredentialsDTO create(AccountCredentialsDTO user) {
        if (user == null) throw new RequiredObjectIsNullException()

        log.info('Creating one new User!')
        User entity = new User(
            fullName: user.fullname,
            userName: user.username,
            password: passwordEncoder.encode(user.password),
            accountNonExpired: true,
            accountNonLocked: true,
            credentialsNonExpired: true,
            enabled: true
        )

        User saved = repository.save(entity)
        new AccountCredentialsDTO(saved.username, saved.password, saved.fullName)
    }
}
