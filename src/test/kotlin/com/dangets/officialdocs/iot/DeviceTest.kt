package com.dangets.officialdocs.iot

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceTest {
    private var system = ActorSystem.create()

    @AfterAll
    fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    @Test
    fun `test reply with empty reading if no temperature is known`() {
        val probe = TestKit(system)

        val deviceActor = system.actorOf(Device.props("test-groupId", "test-deviceId"))
        deviceActor.tell(Device.ReadTemperature(42), probe.ref)

        val response = probe.expectMsgClass(Device.RespondTemperature::class.java)
        val expected = Device.RespondTemperature(42, null)

        assertEquals(expected, response)
    }
}