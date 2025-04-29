# ProtoActor-Kotlin Migration Tutorial

This tutorial provides a step-by-step guide for migrating an existing ProtoActor-Kotlin application to the new API that is compatible with ProtoActor-Go.

## Introduction

In this tutorial, we'll migrate a simple chat application from the old ProtoActor-Kotlin API to the new API. The application consists of:

1. A chat server that manages chat rooms and users
2. Chat clients that connect to the server
3. Remote communication between clients and server

## Prerequisites

- Basic knowledge of ProtoActor-Kotlin
- Kotlin 1.5 or later
- Gradle 7.0 or later

## Starting Code

Let's start with a simple chat application using the old ProtoActor-Kotlin API:

### build.gradle.kts (Old)

```kotlin
dependencies {
    implementation("io.github.proto-actor:proto-actor:0.1.0")
    implementation("io.github.proto-actor:proto-remote:0.1.0")
}
```

### Messages.kt (Old)

```kotlin
package chat

// Chat messages
data class JoinRoom(val roomName: String, val username: String)
data class LeaveRoom(val roomName: String, val username: String)
data class SendMessage(val roomName: String, val username: String, val message: String)
data class ReceiveMessage(val roomName: String, val username: String, val message: String)
```

### ChatServer.kt (Old)

```kotlin
package chat

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.actorOf
import actor.proto.send

class ChatRoomActor : Actor {
    private val users = mutableMapOf<String, PID>()
    
    override suspend fun receive(context: Context) {
        val message = context.message
        when (message) {
            is JoinRoom -> {
                users[message.username] = context.sender!!
                println("${message.username} joined ${message.roomName}")
            }
            is LeaveRoom -> {
                users.remove(message.username)
                println("${message.username} left ${message.roomName}")
            }
            is SendMessage -> {
                val receiveMsg = ReceiveMessage(message.roomName, message.username, message.message)
                users.values.forEach { send(it, receiveMsg) }
                println("${message.username}: ${message.message}")
            }
        }
    }
}

object ChatServer {
    private val rooms = mutableMapOf<String, PID>()
    
    fun start() {
        // Start remote
        actor.proto.remote.Remote.start("localhost", 8090)
        
        // Register the chat room actor
        val props = fromProducer { ChatRoomActor() }
        actor.proto.remote.Remote.register("ChatRoom", props)
        
        println("Chat server started at localhost:8090")
    }
    
    fun getOrCreateRoom(roomName: String): PID {
        return rooms.getOrPut(roomName) {
            val props = fromProducer { ChatRoomActor() }
            actorOf(props, roomName)
        }
    }
}

fun main() {
    ChatServer.start()
    Thread.sleep(Long.MAX_VALUE)
}
```

### ChatClient.kt (Old)

```kotlin
package chat

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.actorOf
import actor.proto.send
import java.util.Scanner

class ChatClientActor : Actor {
    override suspend fun receive(context: Context) {
        val message = context.message
        when (message) {
            is ReceiveMessage -> {
                println("[${message.roomName}] ${message.username}: ${message.message}")
            }
        }
    }
}

class ChatClient(val username: String) {
    private lateinit var clientActor: PID
    private lateinit var roomActor: PID
    private var currentRoom: String? = null
    
    fun start() {
        // Create client actor
        val props = fromProducer { ChatClientActor() }
        clientActor = actorOf(props)
        
        println("Chat client started for user: $username")
    }
    
    fun joinRoom(roomName: String) {
        // Leave current room if any
        currentRoom?.let { leaveRoom() }
        
        // Get remote room actor
        roomActor = PID("localhost:8090", roomName)
        
        // Join room
        send(roomActor, JoinRoom(roomName, username))
        currentRoom = roomName
        
        println("Joined room: $roomName")
    }
    
    fun leaveRoom() {
        currentRoom?.let {
            send(roomActor, LeaveRoom(it, username))
            println("Left room: $it")
            currentRoom = null
        }
    }
    
    fun sendMessage(message: String) {
        currentRoom?.let {
            send(roomActor, SendMessage(it, username, message))
        } ?: println("Not in a room. Join a room first.")
    }
}

fun main() {
    val scanner = Scanner(System.`in`)
    
    print("Enter your username: ")
    val username = scanner.nextLine()
    
    val client = ChatClient(username)
    client.start()
    
    while (true) {
        val line = scanner.nextLine()
        
        when {
            line.startsWith("/join ") -> {
                val roomName = line.substring(6).trim()
                client.joinRoom(roomName)
            }
            line.startsWith("/leave") -> {
                client.leaveRoom()
            }
            line.startsWith("/quit") -> {
                client.leaveRoom()
                break
            }
            else -> {
                client.sendMessage(line)
            }
        }
    }
}
```

## Migration Steps

Now, let's migrate this application to the new ProtoActor-Kotlin API:

### Step 1: Update Dependencies

Update the build.gradle.kts file to use the new version of ProtoActor-Kotlin:

```kotlin
dependencies {
    implementation("io.github.proto-actor:proto-actor:1.0.0")
    implementation("io.github.proto-actor:proto-remote:1.0.0")
}
```

### Step 2: Update Messages.kt

The messages don't need to change, as they're just data classes:

```kotlin
package chat

// Chat messages
data class JoinRoom(val roomName: String, val username: String)
data class LeaveRoom(val roomName: String, val username: String)
data class SendMessage(val roomName: String, val username: String, val message: String)
data class ReceiveMessage(val roomName: String, val username: String, val message: String)
```

### Step 3: Update ChatServer.kt

Update the server to use the new ActorSystem and Remote APIs:

```kotlin
package chat

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.remote.Remote
import actor.proto.remote.RemoteConfig
import actor.proto.send

class ChatRoomActor : Actor {
    private val users = mutableMapOf<String, PID>()
    
    override suspend fun Context.receive(message: Any) {
        when (message) {
            is JoinRoom -> {
                users[message.username] = sender!!
                println("${message.username} joined ${message.roomName}")
            }
            is LeaveRoom -> {
                users.remove(message.username)
                println("${message.username} left ${message.roomName}")
            }
            is SendMessage -> {
                val receiveMsg = ReceiveMessage(message.roomName, message.username, message.message)
                users.values.forEach { send(it, receiveMsg) }
                println("${message.username}: ${message.message}")
            }
        }
    }
}

object ChatServer {
    private val system = ActorSystem("chat-server")
    private val rooms = mutableMapOf<String, PID>()
    private lateinit var remote: Remote
    
    fun start() {
        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 8090)
        remote = Remote.create(system, remoteConfig)
        
        // Register the chat room actor
        val props = fromProducer { ChatRoomActor() }
        remote.registerKind("ChatRoom", props)
        
        // Start remote
        remote.start()
        
        println("Chat server started at localhost:8090")
    }
    
    fun getOrCreateRoom(roomName: String): PID {
        return rooms.getOrPut(roomName) {
            val props = fromProducer { ChatRoomActor() }
            system.actorOf(props, roomName)
        }
    }
}

fun main() {
    ChatServer.start()
    Thread.sleep(Long.MAX_VALUE)
}
```

### Step 4: Update ChatClient.kt

Update the client to use the new ActorSystem and Remote APIs:

```kotlin
package chat

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.remote.Remote
import actor.proto.remote.RemoteConfig
import actor.proto.send
import java.util.Scanner

class ChatClientActor : Actor {
    override suspend fun Context.receive(message: Any) {
        when (message) {
            is ReceiveMessage -> {
                println("[${message.roomName}] ${message.username}: ${message.message}")
            }
        }
    }
}

class ChatClient(val username: String) {
    private val system = ActorSystem("chat-client")
    private lateinit var clientActor: PID
    private lateinit var roomActor: PID
    private var currentRoom: String? = null
    private lateinit var remote: Remote
    
    fun start() {
        // Configure remote
        val remoteConfig = RemoteConfig.create("localhost", 0)
        remote = Remote.create(system, remoteConfig)
        remote.start()
        
        // Create client actor
        val props = fromProducer { ChatClientActor() }
        clientActor = system.actorOf(props)
        
        println("Chat client started for user: $username")
    }
    
    fun joinRoom(roomName: String) {
        // Leave current room if any
        currentRoom?.let { leaveRoom() }
        
        // Get remote room actor
        roomActor = PID("localhost:8090", roomName)
        
        // Join room
        system.send(roomActor, JoinRoom(roomName, username))
        currentRoom = roomName
        
        println("Joined room: $roomName")
    }
    
    fun leaveRoom() {
        currentRoom?.let {
            system.send(roomActor, LeaveRoom(it, username))
            println("Left room: $it")
            currentRoom = null
        }
    }
    
    fun sendMessage(message: String) {
        currentRoom?.let {
            system.send(roomActor, SendMessage(it, username, message))
        } ?: println("Not in a room. Join a room first.")
    }
}

fun main() {
    val scanner = Scanner(System.`in`)
    
    print("Enter your username: ")
    val username = scanner.nextLine()
    
    val client = ChatClient(username)
    client.start()
    
    while (true) {
        val line = scanner.nextLine()
        
        when {
            line.startsWith("/join ") -> {
                val roomName = line.substring(6).trim()
                client.joinRoom(roomName)
            }
            line.startsWith("/leave") -> {
                client.leaveRoom()
            }
            line.startsWith("/quit") -> {
                client.leaveRoom()
                break
            }
            else -> {
                client.sendMessage(line)
            }
        }
    }
}
```

## Key Changes

Let's review the key changes we made:

1. **ActorSystem**: We created an ActorSystem instance instead of using global functions.
   ```kotlin
   private val system = ActorSystem("chat-server")
   ```

2. **Remote Configuration**: We created a RemoteConfig object and a Remote instance.
   ```kotlin
   val remoteConfig = RemoteConfig.create("localhost", 8090)
   remote = Remote.create(system, remoteConfig)
   ```

3. **Actor Creation**: We use the ActorSystem to create actors.
   ```kotlin
   clientActor = system.actorOf(props)
   ```

4. **Message Sending**: We use the ActorSystem to send messages.
   ```kotlin
   system.send(roomActor, JoinRoom(roomName, username))
   ```

5. **Actor Implementation**: We changed the receive method signature.
   ```kotlin
   // Old
   override suspend fun receive(context: Context) {
       val message = context.message
       // ...
   }
   
   // New
   override suspend fun Context.receive(message: Any) {
       // ...
   }
   ```

## Testing the Migration

To test the migration:

1. Start the server:
   ```bash
   ./gradlew run --args="ChatServer"
   ```

2. Start one or more clients:
   ```bash
   ./gradlew run --args="ChatClient"
   ```

3. In each client:
   - Enter a username
   - Join a room with `/join room-name`
   - Send messages
   - Leave the room with `/leave`
   - Quit with `/quit`

## Interoperability with ProtoActor-Go

Now that we've migrated to the new API, our chat application can interoperate with a ProtoActor-Go implementation. Here's a simple Go client that can connect to our Kotlin server:

```go
package main

import (
    "bufio"
    "fmt"
    "os"
    "strings"

    "github.com/asynkron/protoactor-go/actor"
    "github.com/asynkron/protoactor-go/remote"
)

// Chat messages
type JoinRoom struct {
    RoomName string
    Username string
}

type LeaveRoom struct {
    RoomName string
    Username string
}

type SendMessage struct {
    RoomName string
    Username string
    Message  string
}

type ReceiveMessage struct {
    RoomName string
    Username string
    Message  string
}

// ChatClientActor handles incoming messages
type ChatClientActor struct{}

func (c *ChatClientActor) Receive(context actor.Context) {
    switch msg := context.Message().(type) {
    case *ReceiveMessage:
        fmt.Printf("[%s] %s: %s\n", msg.RoomName, msg.Username, msg.Message)
    }
}

func main() {
    // Create actor system
    system := actor.NewActorSystem()

    // Configure remote
    config := remote.Configure("localhost", 0)
    r := remote.NewRemote(system, config)
    r.Start()

    // Create client actor
    props := actor.PropsFromProducer(func() actor.Actor { return &ChatClientActor{} })
    clientPID := system.Root.Spawn(props)

    // Get user input
    reader := bufio.NewReader(os.Stdin)
    fmt.Print("Enter your username: ")
    username, _ := reader.ReadString('\n')
    username = strings.TrimSpace(username)

    fmt.Printf("Chat client started for user: %s\n", username)

    var roomName string
    var roomPID *actor.PID

    for {
        line, _ := reader.ReadString('\n')
        line = strings.TrimSpace(line)

        if strings.HasPrefix(line, "/join ") {
            // Leave current room if any
            if roomPID != nil {
                system.Root.Send(roomPID, &LeaveRoom{RoomName: roomName, Username: username})
                fmt.Printf("Left room: %s\n", roomName)
            }

            // Join new room
            roomName = strings.TrimPrefix(line, "/join ")
            roomPID = actor.NewPID("localhost:8090", roomName)
            system.Root.Send(roomPID, &JoinRoom{RoomName: roomName, Username: username})
            fmt.Printf("Joined room: %s\n", roomName)
        } else if line == "/leave" {
            if roomPID != nil {
                system.Root.Send(roomPID, &LeaveRoom{RoomName: roomName, Username: username})
                fmt.Printf("Left room: %s\n", roomName)
                roomPID = nil
                roomName = ""
            }
        } else if line == "/quit" {
            if roomPID != nil {
                system.Root.Send(roomPID, &LeaveRoom{RoomName: roomName, Username: username})
            }
            break
        } else {
            if roomPID != nil {
                system.Root.Send(roomPID, &SendMessage{RoomName: roomName, Username: username, Message: line})
            } else {
                fmt.Println("Not in a room. Join a room first.")
            }
        }
    }
}
```

## Conclusion

In this tutorial, we've migrated a simple chat application from the old ProtoActor-Kotlin API to the new API that is compatible with ProtoActor-Go. The key changes were:

1. Creating an ActorSystem instance
2. Using the new Remote configuration
3. Updating actor creation and message sending
4. Changing the receive method signature

With these changes, our application can now interoperate with ProtoActor-Go, enabling cross-language actor systems.

For more information, see the [Migration Guide](migration-guide.md) and the [ProtoActor-Kotlin documentation](https://github.com/asynkron/protoactor-kotlin/docs).
