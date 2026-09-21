package br.com.erudio.model

import groovy.transform.EqualsAndHashCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = 'users')
@EqualsAndHashCode
class User implements UserDetails, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(name = 'user_name', unique = true)
    String userName

    @Column(name = 'full_name')
    String fullName

    @Column
    String password

    @Column(name = 'account_non_expired')
    Boolean accountNonExpired

    @Column(name = 'account_non_locked')
    Boolean accountNonLocked

    @Column(name = 'credentials_non_expired')
    Boolean credentialsNonExpired

    @Column
    Boolean enabled

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = 'user_permission',
        joinColumns = [@JoinColumn(name = 'id_user')],
        inverseJoinColumns = [@JoinColumn(name = 'id_permission')]
    )
    List<Permission> permissions

    List<String> getRoles() {
        permissions.collect { Permission permission -> permission.description }
    }

    @Override
    Collection<? extends GrantedAuthority> getAuthorities() {
        permissions
    }

    @Override
    String getUsername() {
        userName
    }

    @Override
    boolean isAccountNonExpired() {
        accountNonExpired
    }

    @Override
    boolean isAccountNonLocked() {
        accountNonLocked
    }

    @Override
    boolean isCredentialsNonExpired() {
        credentialsNonExpired
    }

    @Override
    boolean isEnabled() {
        enabled
    }
}
