package com.dangets

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

fun main() {
    val system = ActorSystem.create("testSystem")

    val firstRef = system.actorOf(KV.props(), "first-actor")
    println("first: $firstRef")
    firstRef.tell("printIt", ActorRef.noSender())

    println(">>> Press ENTER to exit <<<")
    try {
        readLine()
    } finally {
        system.terminate()
    }
}

class KV : AbstractActor() {
    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals("printIt") { msg ->
                val secondRef = context.actorOf(Props.empty(), "second-actor")
                println("second: $secondRef")
            }
            .build()
    }

    companion object {
        //fun props(): Props = Props.create(KV::class.java, ::KV)
        fun props(): Props = Props.create(KV::class.java) { KV() }
    }
}