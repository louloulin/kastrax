# ProtoActor-Kotlin Reentrancy Support

This document describes the reentrancy support in ProtoActor-Kotlin, which allows actors to pause processing while waiting for a future to complete, and then resume processing with the result of the future.

## Overview

Reentrancy is a powerful feature that allows actors to handle asynchronous operations without blocking the actor system. It enables actors to:

1. Send a request to another actor
2. Pause processing while waiting for the response
3. Resume processing with the response when it arrives

This is particularly useful for implementing request-response patterns and complex workflows that involve multiple actors.

## Usage

### Basic Usage

```kotlin
// Create an actor that uses reentrancy
val actor = system.actorOf(fromFunc { msg ->
    when (msg) {
        is String -> {
            if (msg == "start") {
                // Send a request to another actor and get a future
                val future = requestFuture<String>(otherActor, "request", Duration.ofSeconds(5))
                
                // Reenter the actor when the future completes
                reenterAfter(future) { result, error ->
                    if (error == null) {
                        // Process the result
                        println("Received: $result")
                    } else {
                        // Handle the error
                        println("Error: $error")
                    }
                }
            }
        }
    }
})

// Start the process
system.send(actor, "start")
```

### Chaining Requests

You can chain multiple requests using reentrancy:

```kotlin
val actor = system.actorOf(fromFunc { msg ->
    when (msg) {
        is String -> {
            if (msg == "start") {
                val future1 = requestFuture<String>(actor1, "request1", Duration.ofSeconds(5))
                reenterAfter(future1) { res1, err1 ->
                    if (err1 == null) {
                        val future2 = requestFuture<String>(actor2, "request2", Duration.ofSeconds(5))
                        reenterAfter(future2) { res2, err2 ->
                            if (err2 == null) {
                                // Process both results
                                println("Result: $res1 + $res2")
                            }
                        }
                    }
                }
            }
        }
    }
})
```

## How It Works

1. When an actor calls `reenterAfter`, it registers a continuation function to be called when the future completes.
2. The actor continues processing other messages while waiting for the future to complete.
3. When the future completes, a `Continuation` message is sent to the actor.
4. The actor processes the `Continuation` message, which restores the original message context and calls the continuation function.

This approach ensures that the actor's state is preserved between the request and the response, and that the actor can continue processing other messages while waiting for the response.

## Error Handling

The continuation function receives both the result and any error that occurred during the future's execution. This allows you to handle errors gracefully:

```kotlin
reenterAfter(future) { result, error ->
    if (error != null) {
        // Handle the error
        println("Error: $error")
    } else {
        // Process the result
        println("Result: $result")
    }
}
```

## Timeouts

You can specify a timeout when creating a future:

```kotlin
val future = requestFuture<String>(otherActor, "request", Duration.ofSeconds(5))
```

If the future doesn't complete within the specified timeout, it will complete with a `FutureTimeoutException`.

## Compatibility with ProtoActor-Go

This implementation is compatible with the reentrancy support in ProtoActor-Go, allowing for seamless interoperability between Kotlin and Go actors.

## Conclusion

Reentrancy support in ProtoActor-Kotlin provides a powerful way to handle asynchronous operations in actors, enabling complex workflows and request-response patterns without blocking the actor system.
