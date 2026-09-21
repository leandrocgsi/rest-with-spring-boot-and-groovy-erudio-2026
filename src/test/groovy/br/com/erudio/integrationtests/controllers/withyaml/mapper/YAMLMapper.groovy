package br.com.erudio.integrationtests.controllers.withyaml.mapper

import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperDeserializationContext
import io.restassured.mapper.ObjectMapperSerializationContext
import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature

class YAMLMapper implements ObjectMapper {

    private final tools.jackson.dataformat.yaml.YAMLMapper mapper = tools.jackson.dataformat.yaml.YAMLMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    def <T> T read(String content, Class<T> type) {
        try {
            mapper.readValue(content, type)
        } catch (JacksonException e) {
            throw new IllegalArgumentException('Error deserializing YAML content', e)
        }
    }

    @Override
    Object deserialize(ObjectMapperDeserializationContext context) {
        read(context.dataToDeserialize.asString(), context.type as Class)
    }

    @Override
    Object serialize(ObjectMapperSerializationContext context) {
        try {
            mapper.writeValueAsString(context.objectToSerialize)
        } catch (JacksonException e) {
            throw new IllegalArgumentException('Error serializing YAML content', e)
        }
    }
}
