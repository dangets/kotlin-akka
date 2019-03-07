package com.dangets.officialdocs.faulttolerance

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Terminated
import akka.pattern.Patterns
import akka.testkit.javadsl.TestKit
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SupervisorTest {
    private var system = ActorSystem.create()

    @AfterAll
    fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    @Test
    fun `throwing ArithmeticException will keep actor alive (retain state)`() {
        val supervisor = system.actorOf(Supervisor.props)

        val child = Patterns.ask(supervisor, Child.props(), Duration.ofSeconds(5))
            .toCompletableFuture()
            .join() as ActorRef

        child.tell(Child.SetValue(42), ActorRef.noSender())
        assertEquals(42, Patterns.ask(child, Child.GetState, Duration.ofSeconds(5)).toCompletableFuture().join())
        child.tell(ArithmeticException(), ActorRef.noSender())
        assertEquals(42, Patterns.ask(child, Child.GetState, Duration.ofSeconds(5)).toCompletableFuture().join())
    }

    @Test
    fun `throwing NullPointerException will restart actor (lose state)`() {
        val supervisor = system.actorOf(Supervisor.props)

        val child = Patterns.ask(supervisor, Child.props(), Duration.ofSeconds(5))
            .toCompletableFuture()
            .join() as ActorRef

        child.tell(Child.SetValue(42), ActorRef.noSender())
        assertEquals(42, Patterns.ask(child, Child.GetState, Duration.ofSeconds(5)).toCompletableFuture().join())
        child.tell(NullPointerException(), ActorRef.noSender())
        assertEquals(0, Patterns.ask(child, Child.GetState, Duration.ofSeconds(5)).toCompletableFuture().join())
    }

    @Test
    fun `throwing IllegalArgumentException will stop actor`() {
        val supervisor = system.actorOf(Supervisor.props)
        val probe = TestKit(system)

        val child = Patterns.ask(supervisor, Child.props(), Duration.ofSeconds(5))
            .toCompletableFuture()
            .join() as ActorRef

        child.tell(Child.SetValue(42), ActorRef.noSender())
        assertEquals(42, Patterns.ask(child, Child.GetState, Duration.ofSeconds(5)).toCompletableFuture().join())

        probe.watch(child)
        child.tell(IllegalArgumentException(), probe.ref)
        probe.expectMsgClass(Terminated::class.java)
    }

    @Test
    fun `throwing unhandled Exception type will escalate to the supervisor (and stop children)`() {
        val supervisor = system.actorOf(Supervisor.props)
        val probe = TestKit(system)

        val child1 = Patterns.ask(supervisor, Child.props(), Duration.ofSeconds(5))
            .toCompletableFuture()
            .join() as ActorRef

        val child2 = Patterns.ask(supervisor, Child.props(), Duration.ofSeconds(5))
            .toCompletableFuture()
            .join() as ActorRef

        probe.watch(child1)
        probe.watch(child2)

        child1.tell(Exception(), probe.ref)

        val terminationMessageActors = setOf(
            probe.expectMsgClass(Terminated::class.java).actor,
            probe.expectMsgClass(Terminated::class.java).actor
        )

        assertEquals(setOf(child1, child2), terminationMessageActors)

        // killing of all other children happens in the Supervisor's default 'preRestart' method
        //  making this an empty function body will prevent sibling children from dying

        supervisor.tell(Supervisor.GetAllChildren, probe.ref)
        val children = probe.expectMsgClass(Supervisor.RespondAllChildren::class.java)
        println(children)
    }
}