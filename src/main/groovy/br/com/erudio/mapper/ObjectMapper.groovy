package br.com.erudio.mapper

import org.springframework.beans.BeanUtils

final class ObjectMapper {

    private static final String GROOVY_META_CLASS = 'metaClass'

    static <O, D> D parseObject(O origin, Class<D> destination) {
        D target = BeanUtils.instantiateClass(destination)
        BeanUtils.copyProperties(origin, target, GROOVY_META_CLASS)
        target
    }

    static <O, D> List<D> parseListObjects(List<O> origin, Class<D> destination) {
        origin.collect { O item -> parseObject(item, destination) }
    }
}
