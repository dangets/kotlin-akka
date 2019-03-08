package com.dangets.officialdocs.clustering

import akka.actor.AbstractActor
import akka.actor.Props

class StatsWorker : AbstractActor() {
    private val cache = mutableMapOf<String, Int>()

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(String::class.java) { word ->
                val length = cache.computeIfAbsent(word) { it.length }
                sender.tell(length, self)
            }
            .build()
    }

    companion object {
        val props: Props = Props.create(StatsWorker::class.java) { StatsWorker() }
    }
}