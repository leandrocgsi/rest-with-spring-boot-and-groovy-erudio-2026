package br.com.erudio.integrationtests.controllers.withyaml

import br.com.erudio.integrationtests.controllers.AuthControllerSpec
import br.com.erudio.integrationtests.support.RepresentationCodec
import br.com.erudio.integrationtests.support.YamlCodec
import spock.lang.Stepwise

@Stepwise
class AuthControllerYamlSpec extends AuthControllerSpec {

    private static final RepresentationCodec CODEC = new YamlCodec()

    @Override
    RepresentationCodec getCodec() {
        CODEC
    }
}
