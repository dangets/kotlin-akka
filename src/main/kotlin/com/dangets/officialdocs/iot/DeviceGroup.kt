package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging

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
            .match(RequestTrackDevice::class.java) { onTrackDevice(it) }
            .match(Terminated::class.java) { onTerminated(it) }
            .match(RequestDeviceList::class.java) { req ->
                sender.tell(ReplyDeviceList(req.requestId, deviceIdToActor.keys), self)
            }
            .build()
    }

    private fun onTrackDevice(req: RequestTrackDevice) {
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

    private fun onTerminated(msg: Terminated) {
        val ref = msg.actor
        val deviceId = actorToDeviceId.remove(ref)
        if (deviceId == null) {
            log.warning("received Terminated msg for unregistered actor '$ref'")
            return
        }
        log.info("device actor for {} has been terminated", deviceId)
        deviceIdToActor.remove(deviceId)
    }

    companion object {
        fun props(groupId: String): Props = Props.create(DeviceGroup::class.java) { DeviceGroup(groupId) }
    }

    data class RequestDeviceList(val requestId: Long)
    data class ReplyDeviceList(val requestId: Long, val ids: Set<String>)
}