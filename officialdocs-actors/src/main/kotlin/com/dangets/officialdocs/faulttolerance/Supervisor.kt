package com.dangets.officialdocs.faulttolerance

import akka.actor.*
import akka.event.Logging
import akka.japi.pf.DeciderBuilder
import scala.Option
import java.time.Duration

/**
 * See [SupervisorTest] for supervisor strategy semantics
 *
 * Things to remember:
 *  - if error is escalated to Supervisor, by default it will kill all children and *not* auto restart them
 *  -- if you need child "auto" restarting, it needs to happen in the preStart
 *  -- not sure if you can accumulate collection of children needing to be auto recreated ...
 *  - by default remaining children are killed in the preRestart method
 *  -- if you want to avoid killing children you can just put an empty overload for preRestart
 */
class Supervisor : AbstractActor() {
    override fun supervisorStrategy() = strategy

    override fun createReceive(): Receive {
        return receiveBuilder()
            .match(Props::class.java) { props ->
                val aref = context.actorOf(props)
                sender.tell(aref, self)
            }
            .matchEquals(GetAllChildren) { sender.tell(RespondAllChildren(context.children.toList()), self) }
            .build()
    }

    object GetAllChildren
    data class RespondAllChildren(val values: List<ActorRef>)

    companion object {
        val props = Props.create(Supervisor::class.java) { Supervisor() }

        private val strategy = OneForOneStrategy(
            10,
            Duration.ofMinutes(1),
            DeciderBuilder
                .match(ArithmeticException::class.java) { SupervisorStrategy.resume() }
                .match(NullPointerException::class.java) { SupervisorStrategy.restart() }
                .match(IllegalArgumentException::class.java) { SupervisorStrategy.stop() }
                .matchAny { SupervisorStrategy.escalate() }
                .build())
    }
}

class Child(private var state: Int) : AbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    override fun preStart() { log.info("child preStart") }
    override fun preRestart(reason: Throwable?, message: Option<Any>?) { log.info("child preRestart") }
    override fun postStop() { log.info("child postStop") }

    override fun createReceive(): Receive {
        return receiveBuilder()
            // rethrow any exceptions
            .match(Exception::class.java) { throw it }
            // respond to Gets
            .matchEquals(GetState) { sender.tell(state, self) }
            // set internal value
            .match(SetValue::class.java) { state = it.value }
            .build()
    }

    companion object {
        fun props(initialState: Int = 0): Props = Props.create(Child::class.java) { Child(initialState) }
    }

    data class SetValue(val value: Int)
    object GetState
}