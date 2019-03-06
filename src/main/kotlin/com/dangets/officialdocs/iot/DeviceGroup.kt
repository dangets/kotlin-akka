package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.event.Logging

class DeviceGroup(private val groupId: String) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private val deviceIdsToActors = mutableMapOf<String, ActorRef>()

    override fun preStart() {
        log.info("DeviceGroup {} started", groupId)
    }

    override fun postStop() {
        log.info("DeviceGroup {} stopped", groupId)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java) { onTrackDevice(it) }
            .build()
    }

    private fun onTrackDevice(req: RequestTrackDevice) {
        if (groupId != req.groupId) {
            log.warning("Ignoring TrackDevice request for {}.  This actor is responsible for {}.", req.groupId, groupId)
            return
        }
        val deviceId = req.deviceId
        val deviceActor = deviceIdsToActors.computeIfAbsent(deviceId) {
            log.info("creating device actor for {}-{}", groupId, deviceId)
            context.actorOf(Device.props(groupId, deviceId))
        }
        deviceActor.forward(req, context)
    }

    companion object {
        fun props(groupId: String): Props = Props.create(DeviceGroup::class.java) { DeviceGroup(groupId) }
    }
}