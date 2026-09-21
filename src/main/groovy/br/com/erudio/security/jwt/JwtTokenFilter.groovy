package br.com.erudio.security.jwt

import br.com.erudio.exception.InvalidJwtAuthenticationException
import groovy.transform.TupleConstructor
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.GenericFilterBean

@TupleConstructor(includeFields = true, defaults = false)
class JwtTokenFilter extends GenericFilterBean {

    private final JwtTokenProvider tokenProvider

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain filter) {
        String token = tokenProvider.resolveToken((HttpServletRequest) request)
        if (token?.trim() && isValid(token)) {
            Authentication authentication = tokenProvider.getAuthentication(token)
            if (authentication != null) {
                SecurityContextHolder.context.authentication = authentication
            }
        }
        filter.doFilter(request, response)
    }

    private boolean isValid(String token) {
        try {
            tokenProvider.validateToken(token)
        } catch (InvalidJwtAuthenticationException ignored) {
            false
        }
    }
}
