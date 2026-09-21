package br.com.erudio.testsupport

import org.spockframework.mock.IDefaultResponse
import org.spockframework.mock.IMockInvocation
import org.spockframework.mock.ZeroOrNullResponse

class ReturnsSelf implements IDefaultResponse {

    @Override
    Object respond(IMockInvocation invocation) {
        def mock = invocation.mockObject.instance
        invocation.method.returnType.isInstance(mock) ? mock : ZeroOrNullResponse.INSTANCE.respond(invocation)
    }
}
