package com.dangets.rarebooks

sealed class Msg
data class FindBooksByIsbn(val isbn: String, val dateInMillis: Long = System.currentTimeMillis()) : Msg() {
    init {
        require(isbn.isNotBlank()) { "'isbn' cannot be blank" }
    }
}
data class FindBooksByTopic(val topic: String, val dateInMillis: Long = System.currentTimeMillis()) : Msg() {
    init {
        require(topic.isNotBlank()) { "'topic' cannot be blank" }
    }
}

data class BookCard(
    val isbn: String,
    val author: String,
    val title: String,
    val description: String,
    val dateOfOrigin: String,
    val topics: Set<Topic>,
    val publisher: String,
    val language: String,
    val pages: Int
)

sealed class BookQueryResult
object NoBookFound : BookQueryResult()
data class BookFound(val books: List<BookCard>) : BookQueryResult() {
    init {
        require(books.isNotEmpty()) { "'books' cannot be empty" }
    }
}
