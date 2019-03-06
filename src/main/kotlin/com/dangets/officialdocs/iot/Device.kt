package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter

class Device(val groupId: String, val deviceId: String) : AbstractActor() {
    private val log: LoggingAdapter = Logging.getLogger(context.system, self)

    private var lastTemperatureReading: Double? = null

    override fun preStart() {
        log.info("Device actor {}-{} started", groupId, deviceId)
    }

    override fun postStop() {
        log.info("Device actor {}-{} stopped", groupId, deviceId)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(ReadTemperature::class.java) { msg ->
                sender.tell(RespondTemperature(msg.requestId, lastTemperatureReading), self)
            }
            .build()
    }

    companion object {
        fun props(groupId: String, deviceId: String) = Props.create(Device::class.java) { Device(groupId, deviceId) }
    }

    data class ReadTemperature(val requestId: Long)
    data class RespondTemperature(val requestId: Long, val value: Double?)
}