# Persistence in ProtoActor-Kotlin

ProtoActor-Kotlin provides a persistence mechanism for actors, allowing them to recover their state after a crash or restart. This document describes the persistence features and how to use them.

## Overview

The persistence implementation in ProtoActor-Kotlin provides the following features:

- **Event Sourcing**: Actors can persist events and recover their state from these events
- **Snapshots**: Actors can create snapshots of their state to speed up recovery
- **Pluggable Providers**: Different storage backends can be used for persistence

## Getting Started

### Creating a Persistent Actor

To create a persistent actor, extend the `PersistentActor` class:

```kotlin
import actor.proto.Context
import actor.proto.persistence.PersistentActor
import actor.proto.persistence.ReplayComplete
import actor.proto.persistence.RequestSnapshot

class MyPersistentActor : PersistentActor() {
    private val state = mutableListOf<String>()
    
    override suspend fun receiveRecover(context: Context, message: Any) {
        when (message) {
            is String -> state.add(message)
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                state.addAll(message as List<String>)
            }
            is ReplayComplete -> {
                // Recovery complete
                println("Recovery complete, state: $state")
            }
        }
    }
    
    override suspend fun receiveCommand(context: Context, message: Any) {
        when (message) {
            is String -> {
                state.add(message)
                persistReceive(message)
                println("Added message: $message, state: $state")
            }
            is RequestSnapshot -> {
                persistSnapshot(state.toList())
                println("Created snapshot, state: $state")
            }
        }
    }
}
```

### Using a Persistence Provider

To use persistence, you need to create a provider and initialize your actor with it:

```kotlin
import actor.proto.ActorSystem
import actor.proto.persistence.Provider
import actor.proto.persistence.providers.InMemoryProvider
import actor.proto.send

// Create an actor system
val system = ActorSystem("my-system")

// Create a persistence provider
val provider = InMemoryProvider(snapshotInterval = 10)

// Create a persistent actor
val props = fromProducer { MyPersistentActor() }
val pid = system.actorOf(props, "my-actor")

// Initialize the actor with the provider
system.send(pid, provider)

// Send messages to the actor
system.send(pid, "Hello")
system.send(pid, "World")

// Request a snapshot
system.send(pid, RequestSnapshot)
```

### Recovering State

When a persistent actor is restarted, it will automatically recover its state from the persistence provider:

```kotlin
// Create a new actor with the same name
val props2 = fromProducer { MyPersistentActor() }
val pid2 = system.actorOf(props2, "my-actor")

// Initialize the actor with the provider
system.send(pid2, provider)

// The actor will recover its state from the provider
```

## Persistence Providers

ProtoActor-Kotlin supports different persistence providers for different storage backends:

### InMemoryProvider

The `InMemoryProvider` is a simple provider that stores events and snapshots in memory:

```kotlin
val provider = InMemoryProvider(snapshotInterval = 10)
```

### Custom Providers

You can implement your own persistence provider by implementing the `Provider` interface:

```kotlin
class MyProvider(private val snapshotInterval: Int) : Provider {
    private val state = MyProviderState(snapshotInterval)
    
    override fun getState(): ProviderState = state
}

class MyProviderState(private val snapshotInterval: Int) : ProviderState {
    override fun restart() {
        // Implementation
    }
    
    override fun getSnapshotInterval(): Int = snapshotInterval
    
    override fun getSnapshot(actorName: String): Triple<Any?, Int, Boolean> {
        // Implementation
    }
    
    override fun persistSnapshot(actorName: String, snapshotIndex: Int, snapshot: Any) {
        // Implementation
    }
    
    override fun deleteSnapshots(actorName: String, inclusiveToIndex: Int) {
        // Implementation
    }
    
    override fun getEvents(actorName: String, eventIndexStart: Int, eventIndexEnd: Int, callback: (Any) -> Unit) {
        // Implementation
    }
    
    override fun persistEvent(actorName: String, eventIndex: Int, event: Any) {
        // Implementation
    }
    
    override fun deleteEvents(actorName: String, inclusiveToIndex: Int) {
        // Implementation
    }
}
```

## Advanced Features

### Snapshot Intervals

You can configure how often snapshots are created by setting the snapshot interval:

```kotlin
val provider = InMemoryProvider(snapshotInterval = 100)
```

With this configuration, a snapshot will be requested every 100 events.

### Manual Snapshots

You can manually request a snapshot at any time:

```kotlin
system.send(pid, RequestSnapshot)
```

### Event Deletion

You can delete old events after a snapshot has been created:

```kotlin
provider.getState().deleteEvents(actorName, inclusiveToIndex)
```

## Example: Persistent Counter

Here's an example of a persistent counter actor:

```kotlin
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.persistence.PersistentActor
import actor.proto.persistence.ReplayComplete
import actor.proto.persistence.RequestSnapshot
import actor.proto.persistence.providers.InMemoryProvider
import actor.proto.send
import kotlinx.coroutines.runBlocking

// Counter messages
sealed class CounterMessage
object Increment : CounterMessage()
object Decrement : CounterMessage()
object GetCount : CounterMessage()
data class Count(val value: Int)

// Persistent counter actor
class PersistentCounter : PersistentActor() {
    private var count = 0
    
    override suspend fun receiveRecover(context: Context, message: Any) {
        when (message) {
            is Increment -> count++
            is Decrement -> count--
            is Int -> count = message
            is ReplayComplete -> println("Recovery complete, count: $count")
        }
    }
    
    override suspend fun receiveCommand(context: Context, message: Any) {
        when (message) {
            is Increment -> {
                count++
                persistReceive(message)
                println("Incremented, count: $count")
            }
            is Decrement -> {
                count--
                persistReceive(message)
                println("Decremented, count: $count")
            }
            is GetCount -> {
                context.sender?.let { context.send(it, Count(count)) }
            }
            is RequestSnapshot -> {
                persistSnapshot(count)
                println("Created snapshot, count: $count")
            }
        }
    }
}

fun main() = runBlocking {
    // Create an actor system
    val system = ActorSystem("counter-system")
    
    // Create a persistence provider
    val provider = InMemoryProvider(snapshotInterval = 10)
    
    // Create a persistent counter
    val props = fromProducer { PersistentCounter() }
    val pid = system.actorOf(props, "counter")
    
    // Initialize the actor with the provider
    system.send(pid, provider)
    
    // Increment the counter a few times
    repeat(5) {
        system.send(pid, Increment)
    }
    
    // Request a snapshot
    system.send(pid, RequestSnapshot)
    
    // Decrement the counter
    system.send(pid, Decrement)
    
    // Get the count
    val count = system.requestAwait<Count>(pid, GetCount, 1000)
    println("Current count: ${count.value}")
    
    // Create a new counter with the same name
    val props2 = fromProducer { PersistentCounter() }
    val pid2 = system.actorOf(props2, "counter")
    
    // Initialize the actor with the provider
    system.send(pid2, provider)
    
    // The counter will recover its state from the provider
    
    // Get the count after recovery
    val countAfterRecovery = system.requestAwait<Count>(pid2, GetCount, 1000)
    println("Count after recovery: ${countAfterRecovery.value}")
}
```

## Conclusion

The persistence implementation in ProtoActor-Kotlin provides a powerful mechanism for creating actors that can recover their state after a crash or restart. With features like event sourcing, snapshots, and pluggable providers, you can build resilient applications that can survive failures.
