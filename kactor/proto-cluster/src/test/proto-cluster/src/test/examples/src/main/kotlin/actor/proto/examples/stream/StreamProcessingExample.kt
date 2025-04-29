package actor.proto.examples.stream

import actor.proto.ActorSystem
import actor.proto.stream.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating stream processing operations in ProtoActor-Kotlin
 */
fun main() = runBlocking {
    // Create an actor system
    val system = ActorSystem.get("stream-example")

    println("=== TypedStream Example ===")

    // Create a typed stream for integers
    val intStream = TypedStream.create<Int>(system)

    // Send some numbers to the stream
    for (i in 1..10) {
        system.root.send(intStream.pid(), i)
        delay(100) // Simulate some delay between messages
    }

    // Process the stream: filter even numbers, multiply by 2, and take the first 3
    val processedStream = intStream
        .filter(system) { it % 2 == 0 }
        .map(system) { it * 2 }
        .take(system, 3)

    // Collect and print the results
    println("Processed stream results:")
    val results = mutableListOf<Int>()
    for (i in 1..3) {
        val item = processedStream.channel().receive()
        results.add(item)
        println("Received: $item")
    }

    // Calculate the sum using reduce
    val sum = processedStream.reduce(0) { acc, value -> acc + value }
    println("Sum of processed stream: $sum")

    // Create another stream for demonstration
    val anotherStream = TypedStream.create<Int>(system)
    for (i in 11..15) {
        system.root.send(anotherStream.pid(), i)
    }

    // Concatenate streams
    val concatenatedStream = intStream.concat(system, anotherStream)
    println("Concatenated stream (first 5 elements):")
    for (i in 1..5) {
        println("Received: ${concatenatedStream.channel().receive()}")
    }

    println("\n=== UntypedStream Example ===")

    // Create an untyped stream
    val untypedStream = UntypedStream.create(system)

    // Send different types of messages
    system.root.send(untypedStream.pid(), "Hello")
    system.root.send(untypedStream.pid(), 42)
    system.root.send(untypedStream.pid(), true)
    system.root.send(untypedStream.pid(), 3.14)

    // Filter only strings and integers
    val filteredStream = untypedStream.filter(system) {
        it is String || it is Int
    }

    // Map the filtered stream
    val mappedStream = filteredStream.map(system) {
        when (it) {
            is String -> "String: $it"
            is Int -> "Number: $it"
            else -> "Unknown: $it"
        }
    }

    // Print the results
    println("Mapped untyped stream results:")
    for (i in 1..2) {
        println(mappedStream.channel().receive())
    }

    // Clean up
    intStream.close()
    processedStream.close()
    anotherStream.close()
    concatenatedStream.close()
    untypedStream.close()
    filteredStream.close()
    mappedStream.close()

    // No need to explicitly shutdown the actor system

    println("Stream processing example completed.")
}
