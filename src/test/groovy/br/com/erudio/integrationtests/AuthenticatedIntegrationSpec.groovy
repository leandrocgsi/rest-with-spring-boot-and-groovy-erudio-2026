package br.com.erudio.integrationtests

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.dto.AccountCredentialsDTO
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationSpec
import io.restassured.builder.RequestSpecBuilder
import io.restassured.specification.RequestSpecification
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType

import static io.restassured.RestAssured.given

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class AuthenticatedIntegrationSpec extends AbstractIntegrationSpec {

    private static String cachedAccessToken

    protected static RequestSpecification authenticated() {
        new RequestSpecBuilder()
            .setPort(TestConfigs.SERVER_PORT)
            .addHeader(TestConfigs.HEADER_PARAM_AUTHORIZATION, "Bearer ${accessToken()}")
            .build()
    }

    protected static RequestSpecification anonymous() {
        new RequestSpecBuilder()
            .setPort(TestConfigs.SERVER_PORT)
            .build()
    }

    private static synchronized String accessToken() {
        if (cachedAccessToken == null) {
            cachedAccessToken = given()
                .port(TestConfigs.SERVER_PORT)
                .basePath('/auth/signin')
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new AccountCredentialsDTO('leandro', 'admin123'))
                .when()
                    .post()
                .then()
                    .statusCode(200)
                .extract()
                    .path('accessToken')
            assert cachedAccessToken != null: 'signin did not return an access token'
        }
        cachedAccessToken
    }
}
