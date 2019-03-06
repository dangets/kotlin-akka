package com.dangets.officialdocs.basics

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

class SupervisingActor : AbstractActor() {
    private val child = context.actorOf(SupervisedActor.props, "supervised-actor")

    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals("failChild") {
                child.tell("fail", self)
            }
            .build()
    }

    companion object {
        val props: Props = Props.create(SupervisingActor::class.java)
    }
}

class SupervisedActor : AbstractActor() {
    override fun preStart() {
        println("supervised actor started")
    }

    override fun createReceive(): Receive {
        return receiveBuilder()
            .matchEquals("fail") {
                println("supervised actor failing now ...")
                throw IllegalStateException("all is lost")
            }
            .build()
    }

    override fun postStop() {
        println("supervised actor stopped")
    }

    companion object {
        val props: Props = Props.create(SupervisedActor::class.java)
    }
}


fun main() {
    val system = ActorSystem.create("testSystem")

    val supervisor = system.actorOf(SupervisingActor.props)
    supervisor.tell("failChild", ActorRef.noSender())

    println(">>> Press ENTER to exit <<<")
    try {
        readLine()
    } finally {
        system.terminate()
    }
}

