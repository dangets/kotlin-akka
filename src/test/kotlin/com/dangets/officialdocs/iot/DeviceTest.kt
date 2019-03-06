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
    fun `reply with empty reading if no temperature is known`() {
        val probe = TestKit(system)

        val deviceActor = system.actorOf(Device.props("test-groupId", "test-deviceId"))
        deviceActor.tell(Device.ReadTemperature(42), probe.ref)

        val response = probe.expectMsgClass(Device.RespondTemperature::class.java)
        val expected = Device.RespondTemperature(42, null)

        assertEquals(expected, response)
    }

    @Test
    fun `reply with previously set temperature`() {
        val probe = TestKit(system)

        // set a temperature
        val deviceActor = system.actorOf(Device.props("test-groupId", "test-deviceId"))
        deviceActor.tell(Device.RecordTemperature(42, 12.3), probe.ref)

        val actualTempRecorded = probe.expectMsgClass(Device.TemperatureRecorded::class.java)
        val expectedTempRecorded = Device.TemperatureRecorded(42)
        assertEquals(expectedTempRecorded, actualTempRecorded)

        // read the temperature
        deviceActor.tell(Device.ReadTemperature(456), probe.ref)

        val actualRespondTemp = probe.expectMsgClass(Device.RespondTemperature::class.java)
        val expectedRespondTemp = Device.RespondTemperature(456, 12.3)
        assertEquals(expectedRespondTemp, actualRespondTemp)
    }

    @Test
    fun `track device is successful with matching ids`() {
        val probe = TestKit(system)

        val groupId = "groupABC"
        val deviceId = "deviceXYZ"

        val deviceActor = system.actorOf(Device.props(groupId, deviceId))
        deviceActor.tell(RequestTrackDevice(groupId, deviceId), probe.ref)

        val actual = probe.expectMsgClass(DeviceRegistered::class.java)
        val expected = DeviceRegistered
        assertEquals(expected, actual)
    }

    @Test
    fun `track device fails with non-matching ids`() {
        val probe = TestKit(system)

        val groupId = "groupABC"
        val deviceId = "deviceXYZ"

        val deviceActor = system.actorOf(Device.props(groupId, deviceId))
        deviceActor.tell(RequestTrackDevice("badGroup", deviceId), probe.ref)
        probe.expectNoMessage()

        deviceActor.tell(RequestTrackDevice(groupId, "badDevice"), probe.ref)
        probe.expectNoMessage()
    }

}