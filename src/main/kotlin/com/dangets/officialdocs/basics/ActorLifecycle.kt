package com.dangets.officialdocs.basics

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

class StartStopActor(private val count: Int) : AbstractActor() {
    private val name = "Actor$count"

    override fun preStart() {
        println("$name - preStart")
        if (count > 0)
            context.actorOf(props(count - 1))
    }

    override fun postStop() {
        println("$name - postStop")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals("stop") {
                context.stop(self)
            }
            .build()
    }

    companion object {
        fun props(count: Int): Props = Props.create(StartStopActor::class.java, count)
    }
}

fun main() {
    val system = ActorSystem.create("testSystem")

    val a1 = system.actorOf(StartStopActor.props(1))
    a1.tell("stop", ActorRef.noSender())

    println(">>> Press ENTER to exit <<<")
    try {
        readLine()
    } finally {
        system.terminate()
    }
}