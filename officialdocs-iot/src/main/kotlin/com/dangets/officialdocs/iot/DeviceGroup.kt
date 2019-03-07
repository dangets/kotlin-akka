package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging
import java.time.Duration

class DeviceGroup(private val groupId: String) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private val deviceIdToActor = mutableMapOf<String, ActorRef>()
    private val actorToDeviceId = mutableMapOf<ActorRef, String>()

    override fun preStart() {
        log.info("DeviceGroup {} started", groupId)
    }

    override fun postStop() {
        log.info("DeviceGroup {} stopped", groupId)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java) { handleTrackDevice(it) }
            .match(Terminated::class.java) { handleTerminated(it) }
            .match(RequestDeviceList::class.java) { req -> sender.tell(ReplyDeviceList(req.requestId, deviceIdToActor.keys), self) }
            .match(RequestAllTemperatures::class.java) { handleRequestAllTemperatures() }
            .build()
    }

    private fun handleTrackDevice(req: RequestTrackDevice) {
        if (groupId != req.groupId) {
            log.warning("Ignoring TrackDevice request for {}.  This actor is responsible for {}.", req.groupId, groupId)
            return
        }

        val deviceId = req.deviceId
        var deviceActor = deviceIdToActor[deviceId]
        if (deviceActor == null) {
            log.info("creating device actor for {}-{}", groupId, deviceId)
            deviceActor = context.actorOf(Device.props(groupId, deviceId))
            // watch the actor to be notified when the actor stops
            context.watch(deviceActor)
            deviceIdToActor[deviceId] = deviceActor
            actorToDeviceId[deviceActor] = deviceId
        }

        checkNotNull(deviceActor)
        deviceActor.forward(req, context)
    }

    private fun handleTerminated(msg: Terminated) {
        val ref = msg.actor
        val deviceId = actorToDeviceId.remove(ref)
        if (deviceId == null) {
            log.warning("received Terminated msg for unregistered actor '$ref'")
            return
        }
        log.info("device actor for {} has been terminated", deviceId)
        deviceIdToActor.remove(deviceId)
    }

    private fun handleRequestAllTemperatures() {
        // create a TemperatureQueryActor to handle the request and respond back to the sender
        //  if I wanted to be clever and throttle # of requests to devices, should watch this created actor, and forward new requests on to it until it dies
        //    this would require maintaining more state, but would be more efficient
        //  eh... on second thought it might not be that much more efficient
        //    the Device actors are decoupled from actual temperature sampling and essentially cache the last seen value
        val devicesCopy = HashMap(deviceIdToActor)
        val timeout = Duration.ofSeconds(3)
        context.actorOf(TemperatureQueryActor.props(self, -1, sender, devicesCopy, timeout))
    }

    companion object {
        fun props(groupId: String): Props = Props.create(DeviceGroup::class.java) { DeviceGroup(groupId) }
    }

    data class RequestDeviceList(val requestId: Long)
    data class ReplyDeviceList(val requestId: Long, val ids: Set<String>)

    data class RequestAllTemperatures(val requestId: Long)
    data class RespondAllTemperatures(val requestId: Long, val temperatures: Map<String, TemperatureReading>)

    sealed class TemperatureReading {
        data class Ok(val temperature: Double) : TemperatureReading()
        object TemperatureNotAvailable : TemperatureReading()
        object DeviceNotAvailable : TemperatureReading()
        object DeviceTimedOut : TemperatureReading()
    }
}