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
    private val log: LoggingAdapter = Logging.getLogger(context.system, this)

    override fun aroundReceive(receive: PartialFunction<Any, BoxedUnit>?, msg: Any?) {
        if (doLogging) {
            log.debug("{} received message '{}' from {}", self, msg, sender)
        }
        super.aroundReceive(receive, msg)
    }
}

class EchoActor : MyAbstractActor() {
    private val log: LoggingAdapter = Logging.getLogger(context.system, this)

    override fun createReceive(): AbstractActor.Receive {
        return receiveBuilder()
            .match(String::class.java) { log.debug("inside receiveBuilder: '$it'")}
            .build()
    }

    companion object {
        val props: Props = Props.create(EchoActor::class.java) { EchoActor() }
    }
}

/**
 * There is an standard config option "akka.actor.debug.unhandled = on" to log unhandled messages at debug level.
 * This class allows logging unhandled messages at any log level.
 *
 * The reduplication of LogLevel vs. just using Logging.LogLevel is due to type
 *  erasure issues between scala -> kotlin/java (there may be a better way)
 */
class UnhandledMessageLogger private constructor(logLevel: LogLevel) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)
    private val logFn: (template: String, a: Any, b: Any, c: Any) -> Unit = when (logLevel) {
        LogLevel.DEBUG -> { { t, a, b, c -> log.debug(t, a, b, c) } }
        LogLevel.INFO -> { { t, a, b, c -> log.info(t, a, b, c) } }
        LogLevel.WARN -> { { t, a, b, c -> log.warning(t, a, b, c) } }
        LogLevel.ERROR -> { { t, a, b, c -> log.error(t, a, b, c) } }
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(UnhandledMessage::class.java) { msg ->
                logFn("unhandled message {} sent from {} to {}", msg.message, msg.sender, msg.recipient)
            }
            .build()
    }

    private enum class LogLevel { DEBUG, INFO, WARN, ERROR }

    companion object {
        private fun props(logLevel: LogLevel): Props = Props.create(UnhandledMessageLogger::class.java) { UnhandledMessageLogger(logLevel) }
        fun debugProps() = props(LogLevel.DEBUG)
        fun infoProps() = props(LogLevel.INFO)
        fun warnProps() = props(LogLevel.WARN)
        fun errorProps() = props(LogLevel.ERROR)
    }
}

fun main() {
    val config = ConfigFactory.parseString("""
        akka {
            loglevel = "DEBUG"
            actor.debug.receive = on
            # actor.debug.unhandled = on
        }
    """.trimIndent())

    val system = ActorSystem.create("MySystem", config)

    system.eventStream.subscribe(system.actorOf(UnhandledMessageLogger.errorProps()), UnhandledMessage::class.java)

    val echoActor = system.actorOf(EchoActor.props, "joe")
    echoActor.tell("foo", ActorRef.noSender())
    echoActor.tell("bar", ActorRef.noSender())
    echoActor.tell(123, ActorRef.noSender())
    echoActor.tell(PoisonPill.getInstance(), ActorRef.noSender())

    Thread.sleep(1000)

    system.terminate()
}