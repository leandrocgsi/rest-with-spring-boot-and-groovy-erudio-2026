package br.com.erudio.integrationtests.controllers.withxml

import br.com.erudio.integrationtests.controllers.AuthControllerSpec
import br.com.erudio.integrationtests.support.RepresentationCodec
import br.com.erudio.integrationtests.support.XmlCodec
import spock.lang.Stepwise

@Stepwise
class AuthControllerXmlSpec extends AuthControllerSpec {

    private static final RepresentationCodec CODEC = new XmlCodec()

    @Override
    RepresentationCodec getCodec() {
        CODEC
    }
}
