package br.com.erudio.testsupport

import org.springframework.hateoas.RepresentationModel

final class HateoasSupport {

    private HateoasSupport() {}

    static Map<String, Map<String, String>> linksOf(RepresentationModel<?> model) {
        model.links.collectEntries { link ->
            [(link.rel.value()): [href: link.href, type: link.type]]
        }
    }
}
