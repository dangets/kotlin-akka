package com.dangets.officialdocs.clustering

import akka.actor.AbstractActor
import akka.actor.Props
import akka.routing.ConsistentHashingRouter
import akka.routing.FromConfig

class StatsService : AbstractActor() {
    private val workerRouter = context.actorOf(FromConfig.getInstance().props(StatsWorker.props), "workerRouter")

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(
                StatsJob::class.java,
                { job -> job.text.isNotEmpty() },
                { job ->
                    val words = job.text.split(" ")
                    val replyTo = sender
                    val aggregator = context.actorOf(StatsAggregator.props(words.size, replyTo))

                    // distribute the words to the workers
                    words.forEach { word ->
                        workerRouter.tell(ConsistentHashingRouter.ConsistentHashableEnvelope(word, word), aggregator)
                    }
                })
            .build()
    }

    companion object {
        val props: Props = Props.create(StatsService::class.java) { StatsService() }
    }
}