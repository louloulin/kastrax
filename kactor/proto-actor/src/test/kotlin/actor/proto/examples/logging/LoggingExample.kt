package actor.proto.examples.logging

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Props
import actor.proto.withProducer
import actor.proto.withReceiveMiddleware
import actor.proto.withSenderMiddleware
import actor.proto.middleware.loggingReceiveMiddleware
import actor.proto.middleware.loggingSenderMiddleware
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating how to use the logging system
 */
object LoggingExample {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Create an actor system
        val system = ActorSystem.get("logging-example")

        // Create a simple actor that logs messages
        val props = Props().withProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is String -> {
                            // Log using the context logger
                            logger.info("Received string message", "message" to msg)

                            // Send a response
                            if (sender != null) {
                                logger.debug("Sending response to sender", "sender" to sender!!)
                                send(sender!!, "Response to: $msg")
                            }
                        }
                        else -> {
                            logger.warning("Received unknown message type", "type" to msg.javaClass.simpleName)
                        }
                    }
                }
            }
        }

        // Create an actor with logging middleware
        val propsWithMiddleware = Props().withProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is String -> {
                            // The middleware will log the message
                            send(sender!!, "Response from middleware actor: $msg")
                        }
                    }
                }
            }
        }
        .withReceiveMiddleware(loggingReceiveMiddleware(logMessageContent = true))
        .withSenderMiddleware(loggingSenderMiddleware(logMessageContent = true))

        // Create the actors
        val pid = system.actorOf(props, "logger-actor")
        val pidWithMiddleware = system.actorOf(propsWithMiddleware, "middleware-actor")

        println("Sending messages to actors...")

        // Send messages to the actors
        system.send(pid, "Hello, world!")
        system.request(pid, "Hello with request!", system.root.self)

        system.request(pidWithMiddleware, "Hello to middleware actor!", system.root.self)

        // Wait for responses
        delay(100)

        // Send an unknown message type
        system.send(pid, 42)

        // Wait for processing
        delay(100)

        println("Example completed.")
    }
}
