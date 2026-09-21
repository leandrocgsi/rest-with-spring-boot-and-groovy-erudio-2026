package br.com.erudio.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Links
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter
import tools.jackson.dataformat.yaml.YAMLMapper

@Configuration
class JacksonConfig {

    @Bean
    ServerHttpMessageConvertersCustomizer yamlMessageConverterCustomizer() {
        { HttpMessageConverters.ServerBuilder builder ->
            builder.withYamlConverter(new JacksonYamlHttpMessageConverter(
                YAMLMapper.builder().addMixIn(EntityModel, EntityModelYamlMixin)))
        } as ServerHttpMessageConvertersCustomizer
    }

    static interface EntityModelYamlMixin {

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Links getLinks()
    }
}
