package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging

class DeviceManager : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private val groupIdToActor = mutableMapOf<String, ActorRef>()
    private val actorToGroupId = mutableMapOf<ActorRef, String>()

    override fun preStart() {
        log.info("DeviceManager started")
    }

    override fun postStop() {
        log.info("DeviceManager stopped")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(RequestTrackDevice::class.java) { handleRequestTrackDevice(it) }
            .match(Terminated::class.java) { handleTerminated(it) }
            .build()
    }

    private fun handleRequestTrackDevice(req: RequestTrackDevice) {
        var groupRef = groupIdToActor[req.groupId]
        if (groupRef == null) {
            log.info("creating group actor for {}", req.groupId)
            groupRef = context.actorOf(DeviceGroup.props(req.groupId))
            context.watch(groupRef)
            groupIdToActor[req.groupId] = groupRef
            actorToGroupId[groupRef] = req.groupId
        }
        checkNotNull(groupRef)
        groupRef.forward(req, context)
    }

    private fun handleTerminated(req: Terminated) {
        val ref = req.actor
        val groupId = actorToGroupId.remove(ref)
        if (groupId == null) {
            log.warning("received Terminated msg for unregistered actor '$ref'")
            return
        }
        log.info("device group actor for group:{} has been terminated", groupId)
        groupIdToActor.remove(groupId)
    }

    companion object {
        val props: Props = Props.create(DeviceManager::class.java) { DeviceManager() }
    }
}