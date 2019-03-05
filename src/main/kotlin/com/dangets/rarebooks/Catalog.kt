package com.dangets.rarebooks

sealed class Topic
object Africa : Topic()
object Asia : Topic()
object Gilgamesh : Topic()
object Greece : Topic()
object Persia : Topic()
object Philosophy : Topic()
object Royalty : Topic()

object Catalog {
    private val phaedrus = BookCard(
        "0872202208",
        "Plato",
        "Phaedrus",
        "Plato’s enigmatic text that treats a range of important ... issues.",
        "370 BC",
        setOf(Greece, Philosophy),
        "Hackett Publishing Company, Inc.",
        "English",
        144)

    private val theEpicOfGilgamesh = BookCard(
        "0141026286",
        "unknown",
        "The Epic of Gilgamesh",
        "A hero is created by the gods to challenge the arrogant King Gilgamesh.",
        "2700 BC",
        setOf(Gilgamesh, Persia, Royalty),
        "Penguin Classics",
        "English",
        80)

    val allBooks = listOf(phaedrus, theEpicOfGilgamesh)
    val booksByIsbn = allBooks
        .associateBy { it.isbn }
}
