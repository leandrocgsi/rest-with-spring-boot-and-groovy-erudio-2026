package br.com.erudio.security.jwt

import br.com.erudio.data.dto.security.TokenDTO
import br.com.erudio.exception.InvalidJwtAuthenticationException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import groovy.transform.TupleConstructor
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Service
@TupleConstructor(includeFields = true, defaults = false, includes = 'userDetailsService')
class JwtTokenProvider {

    private static final String BEARER_PREFIX = 'Bearer '

    @Value('${security.jwt.token.secret-key:secret}')
    private String secretKey = 'secret'

    @Value('${security.jwt.token.expire-length:3600000}')
    private long validityInMilliseconds = 3600000

    private final UserDetailsService userDetailsService
    private Algorithm algorithm

    @PostConstruct
    protected void init() {
        secretKey = Base64.encoder.encodeToString(secretKey.bytes)
        algorithm = Algorithm.HMAC256(secretKey.bytes)
    }

    TokenDTO createAccessToken(String username, List<String> roles) {
        Date now = new Date()
        Date validity = new Date(now.time + validityInMilliseconds)
        new TokenDTO(username, true, now, validity,
            getAccessToken(username, roles, now, validity),
            getRefreshToken(username, roles, now))
    }

    TokenDTO refreshToken(String refreshToken) {
        String token = refreshTokenContainsBearer(refreshToken) ? refreshToken.substring(BEARER_PREFIX.length()) : ''

        DecodedJWT decodedJWT
        try {
            decodedJWT = JWT.require(algorithm).build().verify(token)
        } catch (JWTVerificationException ignored) {
            throw new InvalidJwtAuthenticationException('Expired or Invalid JWT Token!')
        }

        createAccessToken(decodedJWT.subject, decodedJWT.getClaim('roles').asList(String))
    }

    Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(decodedToken(token).subject)
        new UsernamePasswordAuthenticationToken(userDetails, '', userDetails.authorities)
    }

    String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader('Authorization')
        refreshTokenContainsBearer(bearerToken) ? bearerToken.substring(BEARER_PREFIX.length()) : null
    }

    boolean validateToken(String token) {
        try {
            !decodedToken(token).expiresAt.before(new Date())
        } catch (Exception ignored) {
            throw new InvalidJwtAuthenticationException('Expired or Invalid JWT Token!')
        }
    }

    private String getRefreshToken(String username, List<String> roles, Date now) {
        Date refreshTokenValidity = new Date(now.time + (validityInMilliseconds * 3))
        JWT.create()
            .withClaim('roles', roles)
            .withIssuedAt(now)
            .withExpiresAt(refreshTokenValidity)
            .withSubject(username)
            .sign(algorithm)
    }

    private String getAccessToken(String username, List<String> roles, Date now, Date validity) {
        String issuerUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
        JWT.create()
            .withClaim('roles', roles)
            .withIssuedAt(now)
            .withExpiresAt(validity)
            .withSubject(username)
            .withIssuer(issuerUrl)
            .sign(algorithm)
    }

    private DecodedJWT decodedToken(String token) {
        JWT.require(Algorithm.HMAC256(secretKey.bytes)).build().verify(token)
    }

    private static boolean refreshTokenContainsBearer(String refreshToken) {
        refreshToken?.trim() && refreshToken.startsWith(BEARER_PREFIX)
    }
}
