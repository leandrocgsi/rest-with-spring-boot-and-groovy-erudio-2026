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

@Entity
@Table(name = 'person')
@EqualsAndHashCode
class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(name = 'first_name', nullable = false, length = 80)
    String firstName

    @Column(name = 'last_name', nullable = false, length = 80)
    String lastName

    @Column(nullable = false, length = 100)
    String address

    @Column(nullable = false, length = 6)
    String gender

    @Column(nullable = false)
    Boolean enabled

    @Column(name = 'wikipedia_profile_url', length = 255)
    String profileUrl

    @Column(name = 'photo_url', length = 255)
    String photoUrl

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = 'person_books',
        joinColumns = @JoinColumn(name = 'person_id'),
        inverseJoinColumns = @JoinColumn(name = 'book_id')
    )
    List<Book> books
}
