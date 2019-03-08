package com.dangets.officialdocs.clustering

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Terminated
import akka.testkit.javadsl.TestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StatsAggregatorTest {
    private var system = ActorSystem.create()

    @AfterAll
    fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    @Test
    fun `all responses received`() {
        val probe = TestKit(system)


        val statsAggregator = system.actorOf(StatsAggregator.props(3, probe.ref))
        probe.watch(statsAggregator)

        statsAggregator.tell(1, ActorRef.noSender())
        statsAggregator.tell(2, ActorRef.noSender())
        statsAggregator.tell(3, ActorRef.noSender())

        val result = probe.expectMsgClass(StatsResult::class.java)
        assertEquals(2.0, result.meanWordLength)

        // expect aggregator to shut itself down
        probe.expectMsgClass(Terminated::class.java)
    }

    @Test
    fun `timeout when not all results received`() {
        val probe = TestKit(system)

        val statsAggregator = system.actorOf(StatsAggregator.props(3, probe.ref, Duration.ofSeconds(1)))
        probe.watch(statsAggregator)

        // only sending 2 of the 3 results
        statsAggregator.tell(1, ActorRef.noSender())
        statsAggregator.tell(2, ActorRef.noSender())

        probe.expectMsgClass(JobFailed::class.java)
        // expect aggregator to shut itself down
        probe.expectMsgClass(Terminated::class.java)
    }
}