package com.dangets.officialdocs.clustering

import akka.actor.ActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

fun createSystem(baseConfig: Config, port: Int): ActorSystem {
    val config = ConfigFactory
        .parseString("akka.remote.netty.tcp.port=$port")
        .withFallback(baseConfig)

    val system = ActorSystem.create("ClusterSystem$port", config)
    system.actorOf(SimpleClusterListener.props)

    return system
}

fun main() {
    val seeds = arrayOf(2551, 2552)
    val systems = mutableMapOf<Int, ActorSystem>()

    val baseConfig = ConfigFactory.load()

    seeds.forEach { port ->
        val system = createSystem(baseConfig, port)
        systems[port] = system
    }

    repl@ while (true) {
        val line = readLine()?.trim()?.toLowerCase() ?: break
        if (line.isEmpty())
            continue

        val cmd = parseCommand(line.split(" "))
        when (cmd) {
            Command.Exit -> break@repl
            is Command.Invalid -> { println("invalid command: ${cmd.msg}")}
            is Command.StartSystem -> {
                val system = createSystem(baseConfig, cmd.port)
                systems[cmd.port] = system
            }
            is Command.TerminateSystem -> {
                val system = systems.remove(cmd.port)
                if (system == null) {
                    println("system for port '${cmd.port}' not tracked")
                    continue@repl
                }
                system.terminate()
            }
            is Command.ListSystems -> { println(systems) }
        }
    }

    systems.values.forEach {
        it.terminate()
    }
}

private fun parseCommand(input: List<String>): Command {
    require(input.isNotEmpty())
    val cmd = input[0]
    return try {
        when (cmd) {
            "exit", "quit" -> Command.Exit
            "start" -> Command.StartSystem(input[1].toInt())
            "stop" -> Command.TerminateSystem(input[1].toInt())
            "ls", "list" -> Command.ListSystems
            else -> Command.Invalid("unrecognized command '$cmd'")
        }
    } catch (ex: Exception) {
        Command.Invalid(ex.message ?: "no exception message")
    }
}

private sealed class Command {
    data class Invalid(val msg: String) : Command()
    object Exit : Command()
    data class StartSystem(val port: Int) : Command()
    data class TerminateSystem(val port: Int) : Command()
    object ListSystems : Command()
}
