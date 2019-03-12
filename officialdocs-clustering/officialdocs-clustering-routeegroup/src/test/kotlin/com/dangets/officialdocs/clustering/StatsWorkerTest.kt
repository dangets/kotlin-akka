package com.dangets.officialdocs.clustering

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StatsWorkerTest {
    private var system = ActorSystem.create()

    @AfterAll
    fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    @Test
    fun `worker responds with length of sent string`() {
        val probe = TestKit(system)
        val worker = system.actorOf(StatsWorker.props)

        worker.tell("", probe.ref)
        probe.expectMsg(0)

        worker.tell("a", probe.ref)
        probe.expectMsg(1)

        worker.tell("aa", probe.ref)
        probe.expectMsg(2)
    }
}