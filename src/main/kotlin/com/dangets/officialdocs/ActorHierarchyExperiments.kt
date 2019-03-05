package com.dangets.officialdocs

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

class PrintMyActorRefActor : AbstractActor() {
    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals(PrintYoSelf) {
                val secondRef = context.actorOf(Props.empty(), "second-actor")
                println("second: $secondRef")
            }
            .build()
    }

    companion object {
        val props: Props = Props.create(PrintMyActorRefActor::class.java)

    }

    object PrintYoSelf
}

fun main() {
    val system = ActorSystem.create("testSystem")

    val firstRef = system.actorOf(PrintMyActorRefActor.props, "first-actor")

    println("first:  $firstRef")
    firstRef.tell(PrintMyActorRefActor.PrintYoSelf, ActorRef.noSender())

    println(">>> Press ENTER to exit <<<")

    try {
        readLine()
    } finally {
        println("terminating ...")
        system.terminate()
    }
}

