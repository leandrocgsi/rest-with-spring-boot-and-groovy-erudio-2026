package br.com.erudio.integrationtests.controllers

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.dto.TokenDTO
import br.com.erudio.integrationtests.support.RepresentationCodec
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationSpec
import io.restassured.builder.RequestSpecBuilder
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class AuthControllerSpec extends AbstractIntegrationSpec {

    @Shared
    TokenDTO tokenDto

    abstract RepresentationCodec getCodec()

    def 'signin'() {
        given:
        def credentials = new AccountCredentialsDTO('leandro', 'admin123')
        def specification = new RequestSpecBuilder()
            .setBasePath('/auth/signin')
            .setPort(TestConfigs.SERVER_PORT)
            .build()

        when:
        def content = codec.withBody(codec.request(specification), credentials)
        .when()
            .post()
        .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()
        tokenDto = codec.read(content, TokenDTO)

        then:
        tokenDto.accessToken != null
        tokenDto.refreshToken != null
    }

    def 'refresh token'() {
        given:
        def specification = new RequestSpecBuilder()
            .setBasePath('/auth/refresh')
            .setPort(TestConfigs.SERVER_PORT)
            .build()

        when:
        def content = codec.request(specification)
            .pathParam('username', tokenDto.username)
            .header(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${tokenDto.refreshToken}")
        .when()
            .put('{username}')
        .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()
        tokenDto = codec.read(content, TokenDTO)

        then:
        tokenDto.accessToken != null
        tokenDto.refreshToken != null
    }
}
