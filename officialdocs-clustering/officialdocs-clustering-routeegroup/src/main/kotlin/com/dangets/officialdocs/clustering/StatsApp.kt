package com.dangets.officialdocs.clustering

import akka.actor.AbstractActor
import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.cluster.singleton.ClusterSingletonManager
import akka.cluster.singleton.ClusterSingletonManagerSettings
import akka.cluster.singleton.ClusterSingletonProxy
import akka.cluster.singleton.ClusterSingletonProxySettings
import akka.event.Logging
import com.typesafe.config.ConfigFactory

fun main() {
    val baseConfig = ConfigFactory.load("stats1")

    val config = ConfigFactory
        .parseString("akka.remote.netty.tcp.port=2555")
        .withFallback(ConfigFactory.parseString("""akka.cluster.seed-nodes = [ "akka.tcp://ClusterSystem@127.0.0.1:2555" ]"""))
        .withFallback(ConfigFactory.parseString("akka.cluster.roles = [compute]"))
        .withFallback(baseConfig)

    val system = ActorSystem.create("ClusterSystem", config)

    // create a 'statsService' singleton *somewhere* in the cluster
    val singletonManagerSettings = ClusterSingletonManagerSettings.create(system)
        .withRole("compute")
    system.actorOf(ClusterSingletonManager.props(StatsService.props, PoisonPill.getInstance(), singletonManagerSettings), "statsService")

    // create a proxy to the statsService singleton on each node
    val proxySettings = ClusterSingletonProxySettings.create(system).withRole("compute")
    val service = system.actorOf(ClusterSingletonProxy.props("/user/statsService", proxySettings), "statsServiceProxy")

    val worker = system.actorOf(StatsWorker.props, "statsWorker")
    //val service = system.actorOf(StatsService.props, "statsService")

    val pa = system.actorOf(PrintActor.props)

    try {
        readLine()

        service.tell(StatsJob("lorem ipsum dolor foo bar baz"), pa)

        readLine()
    } finally {
        system.terminate()
    }
}

class PrintActor : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchAny { log.info("received:'$it'  from:$sender") }
            .build()
    }

    companion object {
        val props: Props = Props.create(PrintActor::class.java) { PrintActor() }
    }
}