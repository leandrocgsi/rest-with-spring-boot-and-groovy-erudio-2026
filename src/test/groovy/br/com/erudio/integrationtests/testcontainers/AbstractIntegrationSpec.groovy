package br.com.erudio.integrationtests.testcontainers

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

@ContextConfiguration(initializers = AbstractIntegrationSpec.Initializer)
abstract class AbstractIntegrationSpec extends Specification {

    protected static GreenMail greenMail() {
        Initializer.smtp
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        static final String SMTP_USERNAME = 'sender@erudio.test'
        static final String SMTP_PASSWORD = 'secret'

        static final MySQLContainer mysql = new MySQLContainer('mysql:9.1.0').withConfigurationOverride('mysql-default-conf')

        static final LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse('localstack/localstack:4.11.1'))
            .withServices(LocalStackContainer.Service.S3)

        static final GreenMail smtp = new GreenMail(ServerSetupTest.SMTP.dynamicPort())

        @Override
        void initialize(ConfigurableApplicationContext applicationContext) {
            mysql.start()
            localstack.start()
            startSmtp()
            applicationContext.environment.propertySources.addFirst(
                new MapPropertySource('testcontainers', connectionConfiguration()))
        }

        private static synchronized void startSmtp() {
            if (smtp.running) return

            smtp.start()
            smtp.setUser(SMTP_USERNAME, SMTP_PASSWORD)
            Runtime.runtime.addShutdownHook { smtp.stop() }
        }

        private static Map<String, Object> connectionConfiguration() {
            [
                'spring.datasource.url'                           : mysql.jdbcUrl,
                'spring.datasource.username'                      : mysql.username,
                'spring.datasource.password'                      : mysql.password,
                'spring.mail.host'                                : 'localhost',
                'spring.mail.port'                                : String.valueOf(smtp.smtp.port),
                'spring.mail.username'                            : SMTP_USERNAME,
                'spring.mail.password'                            : SMTP_PASSWORD,
                'spring.mail.properties.mail.smtp.auth'           : 'true',
                'spring.mail.properties.mail.smtp.starttls.enable': 'false',
                'spring.mail.properties.mail.smtp.starttls.required': 'false',
                'aws.s3.bucket'                                   : 'erudio-files-test',
                'aws.s3.region'                                   : localstack.region,
                'aws.s3.endpoint'                                 : localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                'aws.s3.access-key'                                : localstack.accessKey,
                'aws.s3.secret-key'                                : localstack.secretKey
            ]
        }
    }
}
