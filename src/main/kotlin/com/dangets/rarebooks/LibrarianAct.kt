package com.dangets.rarebooks

import akka.actor.AbstractActor
import akka.actor.Props
import java.time.Duration

class LibrarianAct(private val findBookDuration: Duration) : AbstractActor() {

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(FindBooksByIsbn::class.java) { msg ->
                val book = Catalog.booksByIsbn[msg.isbn]
                val reply = when (book) {
                    null -> NoBookFound
                    else -> BookFound(listOf(book))
                }
                sender.tell(reply, self)
            }
            .build()
    }

    companion object {
        fun props(findBookDuration: Duration) = Props.create(LibrarianAct::class.java, findBookDuration)!!
    }
}