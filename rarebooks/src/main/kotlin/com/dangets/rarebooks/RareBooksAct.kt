package com.dangets.rarebooks

import akka.actor.*
import akka.event.Logging
import akka.japi.pf.DeciderBuilder
import java.time.Duration

class RareBooksAct : AbstractActorWithStash() {
    private val log = Logging.getLogger(this)
    private val librarian = createLibrarian()

    override fun supervisorStrategy(): SupervisorStrategy {
        return OneForOneStrategy(10,
            Duration.ofMinutes(1),
            DeciderBuilder
                .match(LibrarianAct.TooManyComplaints::class.java) { SupervisorStrategy.restart() }
                .matchAny { SupervisorStrategy.escalate() }
                .build())
    }

    // start initial state of 'open'
    override fun createReceive() = open

    private var requestsToday: Int = 0
    private var totalRequests: Int = 0

    private val openDuration = Duration.ofSeconds(20)
    private val closeDuration = Duration.ofSeconds(20)
    private val findBookDuration = Duration.ofSeconds(2)

    private val open: Receive = receiveBuilder()
        .match(Msg::class.java) { msg ->
            requestsToday += 1
            librarian.forward(msg, context)
        }
        .matchEquals(Close) {
            context.system.scheduler.scheduleOnce(
                closeDuration.toFiniteDuration(), self, Open,
                context.dispatcher, null)
            context.become(closed)
            self.tell(Report, self)
        }
        .build()

    private val closed: Receive = receiveBuilder()
        .matchEquals(Report) {
            totalRequests += requestsToday
            log.info("requests - today:$requestsToday  total:$totalRequests")
            requestsToday = 0
        }
        .matchEquals(Open) {
            context.system.scheduler.scheduleOnce(
                openDuration.toFiniteDuration(), self, Close,
                context.dispatcher, null)
            unstashAll()
            context.become(open)
        }
        .matchAny { stash() }
        .build()

    private fun createLibrarian(): ActorRef {
        return context.actorOf(LibrarianAct.props(findBookDuration))
    }

    companion object {
        val props = Props.create(RareBooksAct::class.java)

        // messages to self
        object Open
        object Close
        object Report
    }
}