# ProtoActor-Kotlin Stream Processing

This module provides stream processing capabilities for ProtoActor-Kotlin, allowing you to create and manipulate streams of messages.

## Overview

The stream processing module provides two main types of streams:

1. **TypedStream**: A stream that only accepts messages of a specific type.
2. **UntypedStream**: A stream that accepts messages of any type.

Both types of streams can be processed using various operations such as map, filter, reduce, etc.

## Usage

### Creating Streams

```kotlin
// Create an actor system
val system = ActorSystem.create("my-system")

// Create a typed stream for integers
val intStream = TypedStream.create<Int>(system)

// Create an untyped stream
val untypedStream = UntypedStream.create(system)
```

### Sending Messages to Streams

```kotlin
// Send messages to the typed stream
system.root.send(intStream.pid(), 1)
system.root.send(intStream.pid(), 2)
system.root.send(intStream.pid(), 3)

// Send messages to the untyped stream
system.root.send(untypedStream.pid(), "hello")
system.root.send(untypedStream.pid(), 42)
system.root.send(untypedStream.pid(), true)
```

### Processing Streams

```kotlin
// Filter even numbers, multiply by 2, and take the first 3
val processedStream = intStream
    .filter(system) { it % 2 == 0 }
    .map(system) { it * 2 }
    .take(system, 3)

// Collect results
val results = mutableListOf<Int>()
for (i in 1..3) {
    results.add(processedStream.channel().receive())
}

// Calculate sum using reduce
val sum = intStream.reduce(0) { acc, value -> acc + value }
```

### Combining Streams

```kotlin
// Concatenate two streams
val concatenatedStream = stream1.concat(system, stream2)

// Merge two streams
val mergedStream = stream1.merge(system, stream2)
```

### Closing Streams

```kotlin
// Close streams when done
intStream.close()
processedStream.close()
```

## Stream Operations

The following operations are available for both TypedStream and UntypedStream:

- **map**: Transform each element in the stream
- **filter**: Filter elements based on a predicate
- **forEach**: Perform an action on each element
- **collect**: Collect elements into a collection
- **take**: Take a limited number of elements
- **skip**: Skip a number of elements
- **reduce**: Reduce the stream to a single value
- **concat**: Concatenate two streams
- **merge**: Merge two streams

## Example

```kotlin
import actor.proto.ActorSystem
import actor.proto.stream.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create an actor system
    val system = ActorSystem.create("stream-example")
    
    // Create a typed stream for integers
    val intStream = TypedStream.create<Int>(system)
    
    // Send some numbers to the stream
    for (i in 1..10) {
        system.root.send(intStream.pid(), i)
    }
    
    // Process the stream: filter even numbers, multiply by 2, and take the first 3
    val processedStream = intStream
        .filter(system) { it % 2 == 0 }
        .map(system) { it * 2 }
        .take(system, 3)
    
    // Collect and print the results
    println("Processed stream results:")
    for (i in 1..3) {
        println("Received: ${processedStream.channel().receive()}")
    }
    
    // Clean up
    intStream.close()
    processedStream.close()
    
    // Shutdown the actor system
    system.shutdownAsync().join()
}
```

## Compatibility with ProtoActor-Go

This implementation is compatible with ProtoActor-Go's stream processing, allowing for cross-language stream processing between Kotlin and Go.
