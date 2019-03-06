package com.dangets.officialdocs.iot

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.testkit.javadsl.TestKit
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceGroupTest {
    private var system = ActorSystem.create()

    @AfterAll
    fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    @Test
    fun `track new device successful with matching ids`() {
        val probe = TestKit(system)

        val deviceGroup = system.actorOf(DeviceGroup.props("groupABC"))
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceA"), probe.ref)
        probe.expectMsg(DeviceRegistered)
    }

    @Test
    fun `track new device fails with unmatched group id`() {
        val probe = TestKit(system)

        val deviceGroup = system.actorOf(DeviceGroup.props("groupABC"))
        deviceGroup.tell(RequestTrackDevice("groupBAD", "deviceA"), probe.ref)
        probe.expectNoMessage()
    }

    @Test
    fun `create multiple device actors on different device ids`() {
        val probe = TestKit(system)

        val deviceGroup = system.actorOf(DeviceGroup.props("groupABC"))

        // create deviceA
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceA"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val deviceAActor = probe.lastSender

        // create deviceB
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceB"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val deviceBActor = probe.lastSender

        assertNotEquals(deviceAActor, deviceBActor, "expected to have created distinct device actors")

        deviceAActor.tell(Device.RecordTemperature(1, 11.0), probe.ref)
        assertEquals(1, probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId)

        deviceBActor.tell(Device.RecordTemperature(2, 22.0), probe.ref)
        assertEquals(2, probe.expectMsgClass(Device.TemperatureRecorded::class.java).requestId)
    }

    @Test
    fun `expect same actor when registering same device ids`() {
        val probe = TestKit(system)

        val deviceGroup = system.actorOf(DeviceGroup.props("groupABC"))

        // create deviceA
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceA"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val ref1 = probe.lastSender

        // resubmit deviceA
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceA"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val ref2 = probe.lastSender

        assertEquals(ref1, ref2)
    }

    @Test
    fun `list active devices`() {
        val probe = TestKit(system)

        val deviceGroup = system.actorOf(DeviceGroup.props("groupABC"))
        // deviceA
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceA"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val refA = probe.lastSender

        // deviceB
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceB"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val refB = probe.lastSender

        deviceGroup.tell(DeviceGroup.RequestDeviceList(123), probe.ref)
        val deviceList = probe.expectMsgClass(DeviceGroup.ReplyDeviceList::class.java)
        assertEquals(123, deviceList.requestId)
        assertEquals(setOf("deviceA", "deviceB"), deviceList.ids)
    }

    @Test
    fun `list active devices after one terminates`() {
        val probe = TestKit(system)

        val deviceGroup = system.actorOf(DeviceGroup.props("groupABC"))
        // deviceA
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceA"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val refA = probe.lastSender

        // deviceB
        deviceGroup.tell(RequestTrackDevice("groupABC", "deviceB"), probe.ref)
        probe.expectMsg(DeviceRegistered)
        val refB = probe.lastSender

        deviceGroup.tell(DeviceGroup.RequestDeviceList(123), probe.ref)
        val deviceList = probe.expectMsgClass(DeviceGroup.ReplyDeviceList::class.java)
        assertEquals(123, deviceList.requestId)
        assertEquals(setOf("deviceA", "deviceB"), deviceList.ids)

        // now tell one device actor to shut down

        probe.watch(refA)
        refA.tell(PoisonPill.getInstance(), ActorRef.noSender())
        probe.expectTerminated(refA)

        probe.awaitAssert {
            deviceGroup.tell(DeviceGroup.RequestDeviceList(1), probe.ref)
            val newDeviceList = probe.expectMsgClass(DeviceGroup.ReplyDeviceList::class.java)
            assertEquals(1, newDeviceList.requestId)
            assertEquals(setOf("deviceB"), newDeviceList.ids)
        }
    }
}