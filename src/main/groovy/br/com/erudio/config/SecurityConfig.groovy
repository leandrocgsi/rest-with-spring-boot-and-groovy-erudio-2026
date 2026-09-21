package br.com.erudio.config

import br.com.erudio.security.jwt.JwtTokenFilter
import br.com.erudio.security.jwt.JwtTokenProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@EnableWebSecurity
@Configuration
class SecurityConfig {

    private final JwtTokenProvider tokenProvider

    SecurityConfig(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider
    }

    static PasswordEncoder createPasswordEncoder() {
        PasswordEncoder pbkdf2Encoder = new Pbkdf2PasswordEncoder(
            '', 8, 185000,
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256)

        def passwordEncoder = new DelegatingPasswordEncoder('pbkdf2', [pbkdf2: pbkdf2Encoder])
        passwordEncoder.defaultPasswordEncoderForMatches = pbkdf2Encoder
        passwordEncoder
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        createPasswordEncoder()
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        configuration.authenticationManager
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .httpBasic { it.disable() }
            .csrf { it.disable() }
            .addFilterBefore(new JwtTokenFilter(tokenProvider), UsernamePasswordAuthenticationFilter)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers('/auth/signin', '/auth/refresh/**', '/auth/createUser', '/swagger-ui/**', '/v3/api-docs/**').permitAll()
                    .requestMatchers('/api/**').authenticated()
                    .requestMatchers('/users').denyAll()
            }
            .cors(Customizer.withDefaults())
            .build()
    }
}
