package br.com.erudio.integrationtests.swagger

import br.com.erudio.config.TestConfigs
import br.com.erudio.integrationtests.testcontainers.AbstractIntegrationSpec
import org.springframework.boot.test.context.SpringBootTest

import static io.restassured.RestAssured.given

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SwaggerIntegrationSpec extends AbstractIntegrationSpec {

    def 'should display the Swagger UI page'() {
        when:
        def content = given()
            .basePath('/swagger-ui/index.html')
            .port(TestConfigs.SERVER_PORT)
        .when()
            .get()
        .then()
            .statusCode(200)
        .extract()
            .body()
            .asString()

        then:
        content.contains('Swagger UI')
    }
}
