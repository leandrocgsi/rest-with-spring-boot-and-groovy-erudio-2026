package br.com.erudio.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig implements WebMvcConfigurer {

    @Value('${cors.originPatterns:default}')
    private String corsOriginPatterns = ''

    @Override
    void addCorsMappings(CorsRegistry registry) {
        registry.addMapping('/**')
            .allowedOrigins(corsOriginPatterns.split(','))
            .allowedMethods('*')
            .allowCredentials(true)
    }

    @Override
    void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(false)
            .ignoreAcceptHeader(false)
            .useRegisteredExtensionsOnly(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType('json', MediaType.APPLICATION_JSON)
            .mediaType('xml', MediaType.APPLICATION_XML)
            .mediaType('yaml', MediaType.APPLICATION_YAML)
    }
}
