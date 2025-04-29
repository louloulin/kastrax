# Migration Guide for ProtoActor-Kotlin

This document provides guidance for migrating from the old ProtoActor-Kotlin API to the new API that is compatible with ProtoActor-Go.

## Overview

The new version of ProtoActor-Kotlin has been redesigned to be compatible with ProtoActor-Go, enabling seamless interoperability between Kotlin and Go actor systems. This migration guide will help you update your existing code to use the new API.

## Key Changes

### 1. PID Structure

**Old API:**
```kotlin
data class PID(val address: String, val id: String)
```

**New API:**
```kotlin
data class PID(val address: String, val id: String, val requestId: UInt = 0u)
```

The `requestId` field has been added to match the Go implementation. This field is used for request-response patterns.

### 2. Actor System

**Old API:**
```kotlin
// Global functions
fun actorOf(props: Props, name: String? = null): PID
fun send(pid: PID, message: Any)
```

**New API:**
```kotlin
class ActorSystem(val name: String) {
    fun actorOf(props: Props, name: String? = null): PID
    fun send(pid: PID, message: Any)
}

// Create an actor system
val system = ActorSystem("my-system")
```

The new API uses an explicit `ActorSystem` class instead of global functions.

### 3. Message Envelopes

**Old API:**
```kotlin
data class MessageEnvelope(val message: Any, val sender: PID?)
```

**New API:**
```kotlin
data class MessageEnvelope(
    val message: Any,
    val sender: PID?,
    val header: MessageHeader? = null,
    val target: PID? = null
)
```

The new message envelope includes additional fields for headers and target PID.

### 4. Remote Communication

**Old API:**
```kotlin
object Remote {
    fun start(host: String, port: Int)
    fun activateAsync(kind: String, pid: PID): CompletableFuture<PID>
}
```

**New API:**
```kotlin
class Remote private constructor(val system: ActorSystem, val config: RemoteConfig) {
    fun start()
    fun activateAsync(kind: String, pid: PID): CompletableFuture<PID>
    
    companion object {
        fun create(system: ActorSystem, config: RemoteConfig): Remote
    }
}

// Create and start remote
val remoteConfig = RemoteConfig.create("localhost", 8090)
val remote = Remote.create(system, remoteConfig)
remote.start()
```

The new Remote API is tied to an ActorSystem and uses a configuration object.

### 5. Serialization

**Old API:**
```kotlin
object Serialization {
    fun registerSerializer(serializer: Serializer)
}
```

**New API:**
```kotlin
object Serialization {
    fun registerSerializer(serializer: Serializer, serializerId: Int)
    fun registerDefaultSerializer(serializer: Serializer)
}
```

The new API allows specifying a serializer ID and setting a default serializer.

## Migration Steps

### Step 1: Update Dependencies

Update your build.gradle.kts file to use the new version of ProtoActor-Kotlin:

```kotlin
dependencies {
    implementation("io.github.proto-actor:proto-actor:X.Y.Z")
    implementation("io.github.proto-actor:proto-remote:X.Y.Z")
    implementation("io.github.proto-actor:proto-cluster:X.Y.Z")
}
```

### Step 2: Create an Actor System

Replace global function calls with an ActorSystem instance:

**Old code:**
```kotlin
val pid = actorOf(props, "my-actor")
send(pid, "hello")
```

**New code:**
```kotlin
val system = ActorSystem("my-system")
val pid = system.actorOf(props, "my-actor")
system.send(pid, "hello")
```

### Step 3: Update PID Usage

If you're creating PIDs directly, add the requestId parameter:

**Old code:**
```kotlin
val pid = PID("localhost", "actor-1")
```

**New code:**
```kotlin
val pid = PID("localhost", "actor-1", 0u)
```

### Step 4: Update Remote Communication

Replace the old Remote singleton with the new Remote class:

**Old code:**
```kotlin
Remote.start("localhost", 8090)
val remotePid = Remote.activateAsync("MyKind", pid).get()
```

**New code:**
```kotlin
val remoteConfig = RemoteConfig.create("localhost", 8090)
val remote = Remote.create(system, remoteConfig)
remote.start()
val remotePid = remote.activateAsync("MyKind", pid).get()
```

### Step 5: Update Serialization

Update serialization registration to include serializer IDs:

**Old code:**
```kotlin
Serialization.registerSerializer(MySerializer())
```

**New code:**
```kotlin
Serialization.registerSerializer(MySerializer(), 1)
Serialization.registerDefaultSerializer(DefaultSerializer())
```

### Step 6: Update Message Handling

If you're directly handling MessageEnvelope objects, update to include the new fields:

**Old code:**
```kotlin
when (message) {
    is MessageEnvelope -> {
        val msg = message.message
        val sender = message.sender
        // Handle message
    }
}
```

**New code:**
```kotlin
when (message) {
    is MessageEnvelope -> {
        val msg = message.message
        val sender = message.sender
        val header = message.header
        val target = message.target
        // Handle message
    }
}
```

## Using the Compatibility Layer

If you need to maintain backward compatibility with existing code, you can use the provided compatibility layer:

```kotlin
import actor.proto.compat.*

// Use compatibility functions
val pid = actorOfCompat(props, "my-actor")
sendCompat(pid, "hello")
```

The compatibility layer provides functions that match the old API but use the new implementation internally.

## Examples

### Example 1: Basic Actor

**Old code:**
```kotlin
class MyActor : Actor {
    override suspend fun receive(context: Context) {
        val message = context.message
        when (message) {
            is String -> println("Received: $message")
        }
    }
}

val props = fromProducer { MyActor() }
val pid = actorOf(props, "my-actor")
send(pid, "hello")
```

**New code:**
```kotlin
class MyActor : Actor {
    override suspend fun Context.receive(message: Any) {
        when (message) {
            is String -> println("Received: $message")
        }
    }
}

val system = ActorSystem("my-system")
val props = fromProducer { MyActor() }
val pid = system.actorOf(props, "my-actor")
system.send(pid, "hello")
```

### Example 2: Remote Actor

**Old code:**
```kotlin
Remote.start("localhost", 8090)
Remote.register("MyKind", props)

val remotePid = PID("localhost:8090", "remote-actor")
send(remotePid, "hello")
```

**New code:**
```kotlin
val system = ActorSystem("my-system")
val remoteConfig = RemoteConfig.create("localhost", 8090)
val remote = Remote.create(system, remoteConfig)
remote.registerKind("MyKind", props)
remote.start()

val remotePid = PID("localhost:8090", "remote-actor")
system.send(remotePid, "hello")
```

## Troubleshooting

### Common Issues

1. **Missing ActorSystem**: If you see errors about missing methods, you might be trying to use global functions that now require an ActorSystem instance.

2. **Serialization Errors**: If you encounter serialization errors when communicating with Go actors, ensure you're using compatible serializers and have registered them with the correct IDs.

3. **PID Compatibility**: If you're having issues with PIDs, check that you're using the new PID class with the requestId field.

### Getting Help

If you encounter issues during migration, you can:

1. Check the [documentation](https://github.com/asynkron/protoactor-kotlin/docs) for more detailed information
2. Open an issue on the [GitHub repository](https://github.com/asynkron/protoactor-kotlin/issues)
3. Join the [ProtoActor community](https://gitter.im/AsynkronIT/protoactor) for help

## Conclusion

Migrating to the new ProtoActor-Kotlin API will enable your application to interoperate with ProtoActor-Go and take advantage of the new features and improvements. While the migration requires some code changes, the benefits of cross-language compatibility and improved performance make it worthwhile.

The compatibility layer can help ease the transition, allowing you to gradually update your code while maintaining functionality.
