package com.dangets.officialdocs.clustering

import akka.actor.AbstractActor
import akka.actor.Props
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import akka.event.Logging

class SimpleClusterListener : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)
    private val cluster = Cluster.get(context.system)

    override fun preStart() {
        log.info("SimpleClusterListener starting")
        // subscribe to cluster changes
        cluster.subscribe(self, ClusterEvent.initialStateAsEvents(), ClusterEvent.MemberEvent::class.java)
    }

    override fun postStop() {
        log.info("SimpleClusterListener stopped")
        // will re-subscribe when restarted
        cluster.unsubscribe(self)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(ClusterEvent.MemberUp::class.java) { log.info("MemberUp: {}", it.member()) }
            .match(ClusterEvent.UnreachableMember::class.java) { log.info("UnreachableMember: {}", it.member()) }
            .match(ClusterEvent.MemberRemoved::class.java) { log.info("MemberRemoved: {}", it.member()) }
            .match(ClusterEvent.MemberEvent::class.java) { }
            .build()
    }

    companion object {
        val props: Props = Props.create(SimpleClusterListener::class.java) { SimpleClusterListener() }
    }
}