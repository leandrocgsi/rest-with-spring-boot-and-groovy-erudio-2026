package br.com.erudio.services

import br.com.erudio.repository.UserRepository
import groovy.transform.TupleConstructor
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
@TupleConstructor(includeFields = true, defaults = false)
class UserService implements UserDetailsService {

    private final UserRepository repository

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = repository.findByUsername(username)
        if (user == null) throw new UsernameNotFoundException("Username $username not found!")
        user
    }
}
