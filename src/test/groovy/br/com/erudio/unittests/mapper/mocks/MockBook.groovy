package br.com.erudio.unittests.mapper.mocks

import br.com.erudio.data.dto.BookDTO
import br.com.erudio.model.Book

import java.time.LocalDate

class MockBook {

    Book mockEntity(int number = 0) {
        new Book(
            id: number as Long,
            author: "Some Author$number",
            launchDate: LocalDate.now(),
            price: 25D,
            title: "Some Title$number"
        )
    }

    BookDTO mockDTO(int number = 0) {
        new BookDTO(
            id: number as Long,
            author: "Some Author$number",
            launchDate: LocalDate.now(),
            price: 25D,
            title: "Some Title$number"
        )
    }

    List<Book> mockEntityList() {
        (0..<14).collect { int number -> mockEntity(number) }
    }

    List<BookDTO> mockDTOList() {
        (0..<14).collect { int number -> mockDTO(number) }
    }
}
