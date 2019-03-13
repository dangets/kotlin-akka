package com.dangets.officialdocs.clustering.distpubsub

import akka.actor.AbstractActor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator
import akka.event.Logging

class Subscriber : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)
    private val mediator = DistributedPubSub.get(context.system).mediator()

    init {
        mediator.tell(DistributedPubSubMediator.Subscribe("content", self), self)
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(String::class.java) { log.info("Got: {}", it)}
            .match(DistributedPubSubMediator.SubscribeAck::class.java) { log.info("subscribed") }
            .build()
    }
}