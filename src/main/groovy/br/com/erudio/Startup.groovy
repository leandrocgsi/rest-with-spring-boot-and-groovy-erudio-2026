package br.com.erudio

import br.com.erudio.config.SecurityConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Startup {

    static void main(String[] args) {
        SpringApplication.run(Startup, args)
    }

    static void generateHashedPassword() {
        def passwordEncoder = SecurityConfig.createPasswordEncoder()

        println passwordEncoder.encode('admin123')
        println passwordEncoder.encode('admin234')
    }
}
