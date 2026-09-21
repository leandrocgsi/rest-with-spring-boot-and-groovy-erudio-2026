package br.com.erudio.integrationtests.testcontainers

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetupTest
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.mysql.MySQLContainer
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

        static final GreenMail smtp = new GreenMail(ServerSetupTest.SMTP.dynamicPort())

        @Override
        void initialize(ConfigurableApplicationContext applicationContext) {
            mysql.start()
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
                'spring.mail.properties.mail.smtp.starttls.required': 'false'
            ]
        }
    }
}
