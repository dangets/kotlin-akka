package com.dangets

import akka.actor.*
import akka.event.Logging
import java.time.Duration

/**
 * Experimenting with doing a 'watch' on an already stopped actor
 *   - looks like the watcher still receives a 'Terminated' message
 */
fun main() {
    val system = ActorSystem.create()

    val a1 = system.actorOf(Actor1.props)
    val a2 = system.actorOf(Actor1.props)

    system.stop(a2)

    system.scheduler.scheduleOnce(Duration.ofMillis(500), a1,
        Actor1.WatchMe(a2), system.dispatcher, ActorRef.noSender())

    try {
        println(">>> press ENTER to exit <<<")
        readLine()
    } finally {
        system.terminate()
    }
}


class Actor1 : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    override fun postStop() {
        log.info("stopping ...")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(WatchMe::class.java) { context.watch(it.ref) }
            .match(Terminated::class.java) { log.info("recvd Terminated from $sender") }
            .build()
    }

    data class WatchMe(val ref: ActorRef)

    companion object {
        val props = Props.create(Actor1::class.java) { Actor1() }
    }
}