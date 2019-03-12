package com.dangets.officialdocs.clustering

import akka.actor.AbstractActor
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

fun main() {
    val baseConfig = ConfigFactory.load("stats1")
    val config = ConfigFactory
        .parseString("akka.remote.netty.tcp.port=2555")
        .withFallback(baseConfig)

    val system = ActorSystem.create("ClusterSystem", config)

    system.actorOf(StatsWorker.props, "statsWorker")
    val service = system.actorOf(StatsService.props, "statsService")

    val printActor = system.actorOf(Props.create(PrintActor::class.java))

    try {
        println("enter 1")
        readLine()

        service.tell(StatsJob("lorem ipsum dolor"), printActor)

        println(">>> press ENTER to exit <<<")
        readLine()
    } finally {
        system.terminate()
    }
}

class PrintActor : AbstractActor() {
    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchAny { println(it) }
            .build()
    }
}