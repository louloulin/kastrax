package actor.proto.examples.helloworld

import actor.proto.*
import kotlinx.coroutines.runBlocking

class HelloActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is Started -> println("Started")
            is String -> {
                println("Hello $msg")
                stop(self)
            }
            is Stopping -> println("Stopping")
            is Stopped -> println("Stopped")
            else -> println("Unknown message type: ${msg.javaClass.name}")
        }
    }
}

fun main(args: Array<String>) {
    // Create an actor system
    val system = ActorSystem("hello-system")

    // Create a props configuration for the actor
    val props = fromProducer { HelloActor() }

    // Create the actor
    val pid = system.actorOf(props)

    // Send a message to the actor
    system.send(pid, "Proto.Actor")

    // Wait for user input before exiting
    println("Press enter to exit...")
    readLine()

    println("Done!")
}
