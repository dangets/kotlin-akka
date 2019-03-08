package com.dangets.officialdocs.clustering

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ReceiveTimeout
import akka.event.Logging
import java.time.Duration

class StatsAggregator(private val numResultsExpected: Int,
                      private val replyTo: ActorRef,
                      private val timeout: Duration) : AbstractActor() {

    private val log = Logging.getLogger(context.system, this)

    private val results = mutableListOf<Int>()

    override fun preStart() {
        context.receiveTimeout = timeout
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(Integer::class.java) { count ->
                results.add(count.toInt())
                if (results.size == numResultsExpected) {
                    val mean = results.average()
                    replyTo.tell(StatsResult(mean), self)
                    context.stop(self)
                }
            }
            .match(ReceiveTimeout::class.java) {
                replyTo.tell(JobFailed("service unavailable, try again later"), self)
                context.stop(self)
            }
            .matchAny { log.info("HERE! '$it'  ${it.javaClass}")}
            .build()
    }

    companion object {
        fun props(numResultsExpected: Int, replyTo: ActorRef, timeout: Duration = Duration.ofSeconds(3)): Props {
            return Props.create(StatsAggregator::class.java) { StatsAggregator(numResultsExpected, replyTo, timeout) }
        }
    }
}