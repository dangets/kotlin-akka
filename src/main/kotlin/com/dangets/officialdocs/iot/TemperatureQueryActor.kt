package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.event.Logging
import java.time.Duration

class TemperatureQueryActor(private val onBehalfOf: ActorRef,
                            private val requestId: Long,
                            private val replyTo: ActorRef,
                            private val deviceActors: Map<String, ActorRef>,
                            private val timeout: Duration) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private val actorToDeviceId: Map<ActorRef, String> = deviceActors.map { (k, v) -> v to k }.toMap()
    private val deviceIdToResponse = mutableMapOf<String, DeviceGroup.TemperatureReading>()

    private lateinit var timeoutSignal: Cancellable

    override fun preStart() {
        log.info("temperature query actor started")

        // send a query message to all device actors (requestId might not really matter)
        deviceActors.values.forEach { it.tell(Device.ReadTemperature(1), self) }

        // schedule a message to self to signal when the timeout is up
        timeoutSignal = context.system.scheduler.scheduleOnce(timeout, { self.tell(QueryTimeout, self) }, context.dispatcher)
    }

    override fun postStop() {
        log.info("temperature query actor stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals(QueryTimeout) { handleTimeout() }
            .match(Device.RespondTemperature::class.java) { handle(it) }
            .build()
    }

    private fun handle(msg: Device.RespondTemperature) {
        val deviceId = actorToDeviceId[sender]
        if (deviceId == null) {
            log.warning("received temperature response from unregistered actor '$sender'")
            return
        }

        deviceIdToResponse[deviceId] = when (msg.temperature) {
            null -> DeviceGroup.TemperatureReading.TemperatureNotAvailable
            else -> DeviceGroup.TemperatureReading.Ok(msg.temperature)
        }

        // check if all expected replies have come back in
        if (deviceIdToResponse.size == deviceActors.size) {
            timeoutSignal.cancel()
            replyWithResponses()
        }
    }

    private fun handleTimeout() {
        actorToDeviceId.forEach { _, devId ->
            deviceIdToResponse.computeIfAbsent(devId) { DeviceGroup.TemperatureReading.DeviceTimedOut }
        }
        replyWithResponses()
    }

    private fun replyWithResponses() {
        replyTo.tell(DeviceGroup.RespondAllTemperatures(requestId, deviceIdToResponse), onBehalfOf)
        // stop self (and discard any queued messages)
        context.stop(self)
    }

    private object QueryTimeout
}