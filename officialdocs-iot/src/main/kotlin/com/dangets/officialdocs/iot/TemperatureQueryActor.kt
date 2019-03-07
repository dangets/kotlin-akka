package com.dangets.officialdocs.iot

import akka.actor.*
import akka.event.Logging
import java.time.Duration

class TemperatureQueryActor(private val onBehalfOf: ActorRef,
                            private val requestId: Long,
                            private val replyTo: ActorRef,
                            private val deviceActors: Map<String, ActorRef>,
                            private val timeout: Duration) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private val actorToDeviceId: Map<ActorRef, String>
    private val deviceIdToResponse: MutableMap<String, DeviceGroup.TemperatureReading>
    private lateinit var timeoutSignal: Cancellable

    init {
        actorToDeviceId = deviceActors
            .map { (k, v) -> v to k }
            .toMap()
        require(deviceActors.size == actorToDeviceId.size) { "device actorRefs must be unique" }
        deviceIdToResponse = mutableMapOf()
    }

    override fun preStart() {
        log.info("temperature query actor started")

        deviceActors.values.forEach {
            // send a query message to device actor (requestId doesn't really matter here)
            it.tell(Device.ReadTemperature(-1), self)
            // watch the device actor in case it terminates before the timeout
            context.watch(it)
        }

        // schedule a message to self to signal when the timeout is up
        timeoutSignal = context.system.scheduler.scheduleOnce(
            timeout, self, QueryTimeout, context.dispatcher, self)
    }

    override fun postStop() {
        log.info("temperature query actor stopped")
        timeoutSignal.cancel()
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals(QueryTimeout) { handleTimeout() }
            .match(Device.RespondTemperature::class.java) { handleRespondTemperature(it) }
            .match(Terminated::class.java) { handleTerminated() }
            .build()
    }

    private fun handleRespondTemperature(msg: Device.RespondTemperature) {
        val deviceId = actorToDeviceId[sender]
        if (deviceId == null) {
            log.warning("received temperature response from unregistered actor '$sender'")
            return
        }

        deviceIdToResponse[deviceId] = when (msg.temperature) {
            null -> DeviceGroup.TemperatureReading.TemperatureNotAvailable
            else -> DeviceGroup.TemperatureReading.Ok(msg.temperature)
        }

        replyIfAllResponsesIn()
    }

    private fun handleTerminated() {
        val deviceId = actorToDeviceId[sender]
        if (deviceId == null) {
            log.warning("received terminated message from unregistered actor '$sender'")
            return
        }

        deviceIdToResponse[deviceId] = DeviceGroup.TemperatureReading.DeviceNotAvailable
        replyIfAllResponsesIn()
    }

    private fun handleTimeout() {
        actorToDeviceId.forEach { _, devId ->
            deviceIdToResponse.computeIfAbsent(devId) { DeviceGroup.TemperatureReading.DeviceTimedOut }
        }
        replyIfAllResponsesIn()
    }

    private fun replyIfAllResponsesIn() {
        // if all expected replies aren't set - don't do anything
        if (deviceIdToResponse.size < deviceActors.size)
            return

        replyTo.tell(DeviceGroup.RespondAllTemperatures(requestId, deviceIdToResponse), onBehalfOf)
        // stop self (and discard any queued messages)
        context.stop(self)
    }

    companion object {
        fun props(onBehalfOf: ActorRef,
                  requestId: Long,
                  replyTo: ActorRef,
                  deviceActors: Map<String, ActorRef>,
                  timeout: Duration): Props {
            return Props.create(TemperatureQueryActor::class.java) {
                TemperatureQueryActor(
                    onBehalfOf,
                    requestId,
                    replyTo,
                    deviceActors,
                    timeout
                )
            }
        }
    }

    private object QueryTimeout
}