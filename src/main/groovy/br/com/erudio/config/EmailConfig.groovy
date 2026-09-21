package br.com.erudio.config

import groovy.transform.EqualsAndHashCode
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = 'spring.mail')
@EqualsAndHashCode
class EmailConfig {

    String host
    int port
    String username
    String password
    String from
    boolean ssl
}
