package br.com.erudio.integrationtests.controllers.security

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.AuthenticatedIntegrationSpec
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType

import static io.restassured.RestAssured.given
import static org.hamcrest.Matchers.emptyString
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.notNullValue

class InvalidTokenSpec extends AuthenticatedIntegrationSpec {

    @Value('${security.jwt.token.secret-key}')
    String secretKey

    def 'an expired token is rejected like a missing one'() {
        expect:
        assertRejected(expiredToken())
    }

    def 'a token signed with another key is rejected'() {
        expect:
        assertRejected(token('another-secret', new Date(System.currentTimeMillis() + 60_000)))
    }

    def 'a malformed token is rejected'() {
        expect:
        assertRejected('not-a-jwt')
    }

    def 'a valid token is still accepted'() {
        expect:
        given().spec(authenticated())
        .when()
            .get('/api/person/v1')
        .then()
            .statusCode(200)
    }

    def 'an invalid token does not block signin'() {
        expect:
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${expiredToken()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(new AccountCredentialsDTO('leandro', 'admin123'))
        .when()
            .post('/auth/signin')
        .then()
            .statusCode(200)
            .body('accessToken', notNullValue())
    }

    def 'an invalid token does not block the public documentation'() {
        expect:
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${expiredToken()}")
        .when()
            .get('/v3/api-docs')
        .then()
            .statusCode(200)
    }

    def 'an expired refresh token is rejected'() {
        expect:
        assertRefreshRejected("Bearer ${expiredToken()}")
    }

    def 'a malformed refresh token is rejected'() {
        expect:
        assertRefreshRejected('Bearer not-a-jwt')
    }

    def 'a refresh token without the Bearer prefix is rejected'() {
        expect:
        assertRefreshRejected(signin())
    }

    def 'a valid refresh token still works'() {
        expect:
        refreshRequest("Bearer ${signin()}")
            .statusCode(200)
            .body('accessToken', notNullValue())
    }

    private String token(String secret, Date expiresAt) {
        def key = Base64.encoder.encodeToString(secret.bytes)
        JWT.create()
            .withSubject('leandro')
            .withClaim('roles', ['ADMIN'])
            .withIssuedAt(new Date(expiresAt.time - 3_600_000))
            .withExpiresAt(expiresAt)
            .sign(Algorithm.HMAC256(key.bytes))
    }

    private String expiredToken() {
        token(secretKey, new Date(System.currentTimeMillis() - 60_000))
    }

    private static void assertRejected(String bearer) {
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer $bearer")
        .when()
            .get('/api/person/v1')
        .then()
            .statusCode(403)
            .body(emptyString())
    }

    private static refreshRequest(String authorization) {
        given().spec(anonymous())
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, authorization)
            .accept(MediaType.APPLICATION_JSON_VALUE)
        .when()
            .put('/auth/refresh/leandro')
        .then()
    }

    private static void assertRefreshRejected(String authorization) {
        refreshRequest(authorization)
            .statusCode(403)
            .body('message', equalTo('Expired or Invalid JWT Token!'))
    }

    private static String signin() {
        given().spec(anonymous())
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(new AccountCredentialsDTO('leandro', 'admin123'))
        .when()
            .post('/auth/signin')
        .then()
            .statusCode(200)
        .extract()
            .path('refreshToken')
    }
}
