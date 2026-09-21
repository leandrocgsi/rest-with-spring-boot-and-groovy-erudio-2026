package br.com.erudio.integrationtests.controllers.withjson

import br.com.erudio.integrationtests.controllers.AuthControllerSpec
import br.com.erudio.integrationtests.support.JsonCodec
import br.com.erudio.integrationtests.support.RepresentationCodec
import spock.lang.Stepwise

@Stepwise
class AuthControllerJsonSpec extends AuthControllerSpec {

    private static final RepresentationCodec CODEC = new JsonCodec()

    @Override
    RepresentationCodec getCodec() {
        CODEC
    }
}
