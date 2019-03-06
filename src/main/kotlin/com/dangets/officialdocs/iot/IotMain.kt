package com.dangets.officialdocs.iot

import akka.actor.ActorSystem

fun main() {
    val system = ActorSystem.create("iot-system")

    try {
        val iotSupervisor = system.actorOf(IotSupervisor.props, "iot-supervisor")

        println(" >>> press ENTER to exit <<< ")
        readLine()
    } finally {
        system.terminate()
    }
}