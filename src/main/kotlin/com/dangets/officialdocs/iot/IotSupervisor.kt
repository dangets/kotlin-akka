package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter

class IotSupervisor : AbstractActor() {
    val log: LoggingAdapter = Logging.getLogger(context.system, this)

    override fun preStart() {
        log.info("IoT application started")
    }

    override fun postStop() {
        log.info("IoT application stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder().build()
    }

    companion object {
        val props: Props = Props.create(IotSupervisor::class.java)
    }
}