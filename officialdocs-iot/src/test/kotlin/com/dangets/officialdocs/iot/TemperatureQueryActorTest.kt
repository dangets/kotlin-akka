package com.dangets.officialdocs.iot

import akka.actor.AbstractActor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.javadsl.TestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import java.time.Duration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TemperatureQueryActorTest {
    private var system = ActorSystem.create()

    @AfterAll
    private fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    /**
     * Configurable test device actor for testing querying semantics
     * @param duration how long to wait before responding
     * @param temperature what temperature to respond with.  null implies no temperature recorded yet
     */
    class TestDevice(private val duration: Duration,
                     private val temperature: Double?) : AbstractActor() {
        override fun createReceive(): Receive {
            return receiveBuilder()
                .match(Device.ReadTemperature::class.java) {
                    val responseMsg = Device.RespondTemperature(it.requestId, temperature)
                    context.system.scheduler.scheduleOnce(
                        duration, sender, responseMsg,
                        context.dispatcher, self)
                }
                .build()
        }

        companion object {
            fun props(duration: Duration, temperature: Double?): Props {
                return Props.create(TestDevice::class.java) { TestDevice(duration, temperature) }
            }
        }
    }

    @Test
    fun `all devices respond within timeout`() {
        val onBehalfOf = TestKit(system)
        val querier = TestKit(system)

        val d0 = system.actorOf(TestDevice.props(Duration.ofMillis(10), null))
        val d1 = system.actorOf(TestDevice.props(Duration.ofMillis(100), 1.0))
        val d2 = system.actorOf(TestDevice.props(Duration.ofMillis(200), 2.0))
        val d3 = system.actorOf(TestDevice.props(Duration.ofMillis(300), 3.0))

        val queryRequestId = 42L
        val deviceMap = mapOf("d0" to d0, "d1" to d1, "d2" to d2, "d3" to d3)

        val queryActor = system.actorOf(TemperatureQueryActor.props(
            onBehalfOf = onBehalfOf.ref,
            deviceActors = deviceMap,
            replyTo = querier.ref,
            requestId = queryRequestId,
            timeout = Duration.ofMillis(400)
        ))

        val response = querier.expectMsgClass(Duration.ofMillis(400),
            DeviceGroup.RespondAllTemperatures::class.java)

        assertAll(
            { assertEquals(onBehalfOf.ref, querier.lastSender, "query reply should come from onBehalfOf") },
            { assertEquals(queryRequestId, response.requestId) },
            { assertEquals(4, response.temperatures.size) },
            { assertEquals(DeviceGroup.TemperatureReading.TemperatureNotAvailable, response.temperatures["d0"]) },
            { assertEquals(DeviceGroup.TemperatureReading.Ok(1.0), response.temperatures["d1"]) },
            { assertEquals(DeviceGroup.TemperatureReading.Ok(2.0), response.temperatures["d2"]) },
            { assertEquals(DeviceGroup.TemperatureReading.Ok(3.0), response.temperatures["d3"]) }
        )
    }

    @Test
    fun `timeout works`() {
        val probe = TestKit(system)

        val timeout = Duration.ofMillis(200)

        val d1 = system.actorOf(TestDevice.props(Duration.ofMillis(100), 1.0))
        val d3 = system.actorOf(TestDevice.props(Duration.ofMillis(300), 3.0))

        val queryRequestId = 42L
        val deviceMap = mapOf("d1" to d1, "d3" to d3)

        val queryActor = system.actorOf(TemperatureQueryActor.props(
            onBehalfOf = probe.ref,  // Actor.noSender == null which blows up Kotlin nullability
            deviceActors = deviceMap,
            replyTo = probe.ref,
            requestId = queryRequestId,
            timeout = timeout
        ))

        val response = probe.expectMsgClass(timeout.plusMillis(50),
            DeviceGroup.RespondAllTemperatures::class.java)

        assertAll(
            { assertEquals(queryRequestId, response.requestId) },
            { assertEquals(2, response.temperatures.size) },
            { assertEquals(DeviceGroup.TemperatureReading.Ok(1.0), response.temperatures["d1"]) },
            { assertEquals(DeviceGroup.TemperatureReading.DeviceTimedOut, response.temperatures["d3"]) }
        )
    }

    @Test
    fun `device actor stops during query and completes response`() {
        val probe = TestKit(system)

        val timeout = Duration.ofSeconds(200)

        val d1 = system.actorOf(TestDevice.props(Duration.ofMillis(100), 1.0))
        val d2 = system.actorOf(TestDevice.props(Duration.ofMillis(200), 2.0))
        val d3 = system.actorOf(TestDevice.props(Duration.ofSeconds(3), 3.0))

        val queryRequestId = 42L
        val deviceMap = mapOf("d1" to d1, "d2" to d2, "d3" to d3)

        val queryActor = system.actorOf(TemperatureQueryActor.props(
            onBehalfOf = probe.ref,  // Actor.noSender == null which blows up Kotlin nullability
            deviceActors = deviceMap,
            replyTo = probe.ref,
            requestId = queryRequestId,
            timeout = timeout
        ))

        // explicitly stop d3 when it should be the last response
        system.scheduler.scheduleOnce(Duration.ofMillis(300), { system.stop(d3) }, system.dispatcher)

        val response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures::class.java)

        assertAll(
            { assertEquals(queryRequestId, response.requestId) },
            { assertEquals(3, response.temperatures.size) },
            { assertEquals(DeviceGroup.TemperatureReading.Ok(1.0), response.temperatures["d1"]) },
            { assertEquals(DeviceGroup.TemperatureReading.Ok(2.0), response.temperatures["d2"]) },
            { assertEquals(DeviceGroup.TemperatureReading.DeviceNotAvailable, response.temperatures["d3"]) }
        )
    }
}