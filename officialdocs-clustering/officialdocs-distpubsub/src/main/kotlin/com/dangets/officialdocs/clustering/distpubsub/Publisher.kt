package com.dangets.officialdocs.clustering.distpubsub

import akka.actor.AbstractActor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator

class Publisher : AbstractActor() {
    private val mediator = DistributedPubSub.get(context.system).mediator()

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(String::class.java) {
                val msg = it.toUpperCase()
                val topic = "content"
                mediator.tell(DistributedPubSubMediator.Publish(topic, msg), self)
            }
            .build()
    }
}