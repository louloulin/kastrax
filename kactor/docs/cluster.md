# Cluster Support in ProtoActor-Kotlin

ProtoActor-Kotlin provides a comprehensive cluster implementation for building distributed actor systems. This document describes the cluster features and how to use them.

## Overview

The cluster implementation in ProtoActor-Kotlin provides the following features:

- **Cluster Membership**: Automatic discovery and monitoring of cluster nodes
- **Virtual Actors**: Location-transparent actors that can be accessed from anywhere in the cluster
- **Gossip Protocol**: Efficient state synchronization between cluster nodes
- **Publish-Subscribe**: Distributed pub-sub messaging for cluster-wide communication

## Getting Started

### Creating a Cluster

To create a cluster, you need to configure a cluster provider and an identity lookup:

```kotlin
import actor.proto.ActorSystem
import actor.proto.cluster.*
import actor.proto.cluster.providers.*
import actor.proto.remote.*

// Create an actor system
val system = ActorSystem("my-system")

// Configure remote
val remoteConfig = RemoteConfig.create("localhost", 8090)

// Configure cluster provider
val clusterProvider = AutoManagedClusterProvider(
    port = 8090,
    seedNodes = listOf("localhost:8090", "localhost:8091")
)

// Configure identity lookup
val identityLookup = DistributedHashIdentityLookup()

// Configure cluster
val clusterConfig = ClusterConfig.create(
    name = "my-cluster",
    clusterProvider = clusterProvider,
    identityLookup = identityLookup,
    remoteConfig = remoteConfig,
    WithRequestTimeout(Duration.ofSeconds(5)),
    WithGossipInterval(Duration.ofMillis(300))
)

// Create cluster
val cluster = Cluster.create(system, clusterConfig)

// Start cluster as a member
cluster.startMember()
```

### Registering Actor Kinds

Before you can use virtual actors, you need to register the actor kinds with the cluster:

```kotlin
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.cluster.Kind

// Define an actor
class MyActor : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is String -> println("Received: $msg")
        }
    }
}

// Create props for the actor
val props = fromProducer { MyActor() }

// Register the kind with the cluster
val kind = Kind("my-actor", props)
cluster.registerKind(kind)
```

### Using Virtual Actors

Virtual actors are location-transparent actors that can be accessed from anywhere in the cluster:

```kotlin
import actor.proto.PID
import actor.proto.send
import actor.proto.cluster.ClusterIdentity

// Get a virtual actor by identity and kind
val pid: PID = cluster.get("actor-1", "my-actor")

// Send a message to the actor
system.send(pid, "Hello, virtual actor!")

// You can also use a cluster identity
val identity = ClusterIdentity("actor-2", "my-actor")
val pid2: PID = cluster.get(identity)
system.send(pid2, "Hello, another virtual actor!")
```

### Using Publish-Subscribe

The cluster provides a publish-subscribe mechanism for cluster-wide messaging:

```kotlin
import actor.proto.PID
import actor.proto.cluster.PubSubMessage

// Subscribe to a topic
cluster.pubSub.subscribe("my-topic", myActorPid)

// Publish a message to a topic
cluster.pubSub.publish("my-topic", "Hello, subscribers!")

// Unsubscribe from a topic
cluster.pubSub.unsubscribe("my-topic", myActorPid)
```

## Cluster Providers

ProtoActor-Kotlin supports different cluster providers for different environments:

### AutoManagedClusterProvider

The `AutoManagedClusterProvider` is a simple provider that uses a gossip protocol to discover and monitor cluster nodes:

```kotlin
val clusterProvider = AutoManagedClusterProvider(
    port = 8090,
    seedNodes = listOf("localhost:8090", "localhost:8091")
)
```

### Custom Providers

You can implement your own cluster provider by implementing the `ClusterProvider` interface:

```kotlin
class MyClusterProvider : ClusterProvider {
    override suspend fun startMember(cluster: Cluster): Boolean {
        // Implementation
    }
    
    override suspend fun startClient(cluster: Cluster): Boolean {
        // Implementation
    }
    
    override suspend fun shutdown(graceful: Boolean): Boolean {
        // Implementation
    }
}
```

## Identity Lookups

Identity lookups are responsible for locating virtual actors in the cluster:

### DistributedHashIdentityLookup

The `DistributedHashIdentityLookup` uses distributed hashing to locate actors:

```kotlin
val identityLookup = DistributedHashIdentityLookup()
```

### Custom Lookups

You can implement your own identity lookup by implementing the `IdentityLookup` interface:

```kotlin
class MyIdentityLookup : IdentityLookup {
    override suspend fun setup(cluster: Cluster, kinds: Map<String, Kind>, isClient: Boolean) {
        // Implementation
    }
    
    override suspend fun lookup(clusterIdentity: ClusterIdentity): PID {
        // Implementation
    }
}
```

## Member Strategies

Member strategies determine which member should host a virtual actor:

### RoundRobinMemberStrategy

The `RoundRobinMemberStrategy` selects members in a round-robin fashion:

```kotlin
val strategy = RoundRobinMemberStrategy()
```

### RendezvousMemberStrategy

The `RendezvousMemberStrategy` uses the rendezvous hashing algorithm to select members:

```kotlin
val strategy = RendezvousMemberStrategy()
```

### Custom Strategies

You can implement your own member strategy by implementing the `MemberStrategy` interface:

```kotlin
class MyMemberStrategy : MemberStrategy {
    override fun addMember(memberId: String) {
        // Implementation
    }
    
    override fun removeMember(memberId: String) {
        // Implementation
    }
    
    override fun getPartition(identity: String): String? {
        // Implementation
    }
}
```

## Gossip Protocol

The cluster uses a gossip protocol to synchronize state between nodes:

```kotlin
// Update the gossip state
cluster.gossip.updateState("my-key", "my-value")

// Get the gossip state
val state = cluster.gossip.getState("my-key")

// Register a consensus check
val consensusCheck = cluster.gossip.registerConsensusCheck("my-key") { value ->
    // Extract a comparable value from the gossip state
    value.toString().length
}

// Check for consensus
val (value, hasConsensus) = consensusCheck.tryGetConsensus()
if (hasConsensus) {
    println("Consensus reached: $value")
}
```

## Example: Cluster Chat

Here's an example of a simple chat application using the cluster:

```kotlin
import actor.proto.*
import actor.proto.cluster.*
import actor.proto.cluster.providers.*
import actor.proto.remote.*
import kotlinx.coroutines.runBlocking
import java.time.Duration

// Chat message
data class ChatMessage(val from: String, val text: String)

// Chat actor
class ChatActor(private val name: String) : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is ChatMessage -> println("[$name] ${msg.from}: ${msg.text}")
            is String -> {
                // Publish the message to the chat topic
                val cluster = Cluster.get(actorSystem)
                cluster.pubSub.publish("chat", ChatMessage(name, msg))
            }
        }
    }
}

fun main() = runBlocking {
    // Create an actor system
    val system = ActorSystem("chat-system")
    
    // Configure remote
    val remoteConfig = RemoteConfig.create("localhost", 8090)
    
    // Configure cluster provider
    val clusterProvider = AutoManagedClusterProvider(
        port = 8090,
        seedNodes = listOf("localhost:8090")
    )
    
    // Configure identity lookup
    val identityLookup = DistributedHashIdentityLookup()
    
    // Configure cluster
    val clusterConfig = ClusterConfig.create(
        name = "chat-cluster",
        clusterProvider = clusterProvider,
        identityLookup = identityLookup,
        remoteConfig = remoteConfig
    )
    
    // Create cluster
    val cluster = Cluster.create(system, clusterConfig)
    
    // Register the chat actor kind
    val props = fromFunc { ctx, msg ->
        val name = ctx.self.id
        ChatActor(name).apply { ctx.receive(msg) }
    }
    cluster.registerKind(Kind("chat-actor", props))
    
    // Start cluster as a member
    cluster.startMember()
    
    // Create a chat actor
    val name = readLine() ?: "Anonymous"
    val chatActor = cluster.get(name, "chat-actor")
    
    // Subscribe to the chat topic
    cluster.pubSub.subscribe("chat", chatActor)
    
    // Send messages
    while (true) {
        val text = readLine() ?: break
        system.send(chatActor, text)
    }
    
    // Shutdown the cluster
    cluster.shutdown(true)
}
```

## Conclusion

The cluster implementation in ProtoActor-Kotlin provides a powerful foundation for building distributed actor systems. With features like virtual actors, gossip protocol, and publish-subscribe, you can build scalable and resilient applications that span multiple nodes.
