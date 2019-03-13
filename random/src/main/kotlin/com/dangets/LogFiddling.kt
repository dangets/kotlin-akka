package com.dangets

import akka.actor.*
import akka.event.Logging
import akka.event.LoggingAdapter
import com.typesafe.config.ConfigFactory
import scala.PartialFunction
import scala.runtime.BoxedUnit

/*
 * The config option 'akka.actor.debug.receive = on' only works for Scala api
 *  https://stackoverflow.com/questions/32639947/logging-received-messages-in-akka
 *
 * To support this in java you can create an abstract class and override the 'aroundReceive' method
 */

abstract class MyAbstractActor : AbstractActor() {
    private val doLogging = context.system.settings().AddLoggingReceive()
    abstract val log: LoggingAdapter

    override fun aroundReceive(receive: PartialFunction<Any, BoxedUnit>?, msg: Any?) {
        if (doLogging) {
            log.debug("{} received message '{}' from {}", self, msg, sender)
        }
        super.aroundReceive(receive, msg)
    }
}

class EchoActor : MyAbstractActor() {
    override val log = Logging.getLogger(context.system, this)

    override fun createReceive(): AbstractActor.Receive {
        return receiveBuilder()
            .match(String::class.java) { log.debug("inside receiveBuilder: '$it'")}
            .build()
    }

    companion object {
        val props: Props = Props.create(EchoActor::class.java) { EchoActor() }
    }
}

fun main() {
    val config = ConfigFactory.parseString("""
        akka {
            loglevel = "DEBUG"
            actor.debug.receive = on
        }
    """.trimIndent())

    val system = ActorSystem.create("MySystem", config)

    val echoActor = system.actorOf(EchoActor.props)
    echoActor.tell("foo", ActorRef.noSender())
    echoActor.tell("bar", ActorRef.noSender())
    echoActor.tell(PoisonPill.getInstance(), ActorRef.noSender())

    system.terminate()
}