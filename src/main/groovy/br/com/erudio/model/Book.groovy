package br.com.erudio.model

import groovy.transform.EqualsAndHashCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

import java.time.LocalDate

@Entity
@Table(name = 'books')
@EqualsAndHashCode
class Book implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Column(nullable = false, length = 180)
    String author

    @Column(name = 'launch_date', nullable = false)
    LocalDate launchDate

    @Column(nullable = false)
    Double price

    @Column(nullable = false, length = 250)
    String title
}
