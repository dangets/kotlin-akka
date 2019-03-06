package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging
import akka.event.LoggingAdapter

class Device(private val groupId: String, private val deviceId: String) : AbstractActor() {
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
            .match(RequestTrackDevice::class.java) { msg ->
                if (groupId == msg.groupId && deviceId == msg.deviceId) {
                    sender.tell(DeviceRegistered, self)
                } else {
                    log.warning("Ignoring track device request for {}-{}.  This actor is responsible for {}-{}.", msg.groupId, msg.deviceId, groupId, deviceId)
                }
            }
            .match(RecordTemperature::class.java) { msg ->
                lastTemperatureReading = msg.temperature
                log.info("recorded temperature reading {} of {}", msg.requestId, msg.temperature)
                sender.tell(TemperatureRecorded(msg.requestId), self)
            }
            .match(ReadTemperature::class.java) { msg ->
                sender.tell(RespondTemperature(msg.requestId, lastTemperatureReading), self)
            }
            .build()
    }

    companion object {
        fun props(groupId: String, deviceId: String) = Props.create(Device::class.java) { Device(groupId, deviceId) }
    }

    data class ReadTemperature(val requestId: Long)
    data class RespondTemperature(val requestId: Long, val temperature: Double?)
    data class RecordTemperature(val requestId: Long, val temperature: Double)
    data class TemperatureRecorded(val requestId: Long)
}