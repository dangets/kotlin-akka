package com.dangets.officialdocs.iot

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceManagerTest {
    private var system = ActorSystem.create()

    @AfterAll
    fun tearDown() {
        TestKit.shutdownActorSystem(system)
        system = null
    }

    @Nested
    inner class RequestTrackDevice {
        /**
         * Test creation of children under same group (DeviceManager -> DeviceGroup -> Device)
         */
        @Test
        fun `duplicate RequestTrackDevice messages should create single device actor`() {
            val probe = TestKit(system)
            val devMgr = system.actorOf(DeviceManager.props)

            val requestTrackMsg = RequestTrackDevice("groupA", "device1")

            devMgr.tell(requestTrackMsg, probe.ref)
            probe.expectMsg(DeviceRegistered)
            val ref1 = probe.lastSender

            devMgr.tell(requestTrackMsg, probe.ref)
            probe.expectMsg(DeviceRegistered)
            val ref2 = probe.lastSender

            assertEquals(ref1, ref2)
        }

        @Test
        fun `RequestTrackDevice with different groupIds`() {
            val probe = TestKit(system)
            val devMgr = system.actorOf(DeviceManager.props)

            devMgr.tell(RequestTrackDevice("groupA", "device1"), probe.ref)
            probe.expectMsg(DeviceRegistered)
            val deviceRef1 = probe.lastSender

            devMgr.tell(RequestTrackDevice("groupB", "device1"), probe.ref)
            probe.expectMsg(DeviceRegistered)
            val deviceRef2 = probe.lastSender

            assertNotEquals(deviceRef1, deviceRef2)
        }
    }
}