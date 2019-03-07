package com.dangets.rarebooks

import akka.actor.AbstractActorWithStash
import akka.actor.ActorRef
import akka.actor.Props
import java.time.Duration

class LibrarianAct(private val findBookDuration: Duration) : AbstractActorWithStash() {

    override fun createReceive(): Receive = ready

    private val ready: Receive = receiveBuilder()
        .match(FindBooksByIsbn::class.java) { msg ->
            val book = Catalog.booksByIsbn[msg.isbn]
            val reply = when (book) {
                null -> NoBookFound
                else -> BookFound(listOf(book))
            }

            context.system.scheduler.scheduleOnce(findBookDuration,
                self, BusyDone(reply, sender), context.dispatcher, self)
            context.become(busy)
        }
        .build()

    private val busy: Receive = receiveBuilder()
        .match(Companion.BusyDone::class.java) { msg ->
            msg.customer.tell(msg.bookQueryResult, self)
            unstashAll()
            context.become(ready)
        }
        .matchAny { stash() }
        .build()

    companion object {
        data class BusyDone(val bookQueryResult: BookQueryResult, val customer: ActorRef)

        fun props(findBookDuration: Duration) = Props.create(LibrarianAct::class.java, findBookDuration)!!
    }

    class TooManyComplaints(val customer: ActorRef) : IllegalStateException("too many complaints")
}