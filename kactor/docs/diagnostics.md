# Diagnostics in ProtoActor-Kotlin

ProtoActor-Kotlin provides a comprehensive diagnostics API for monitoring and troubleshooting your actor system. This document describes the diagnostics features and how to use them.

## Local Diagnostics

The `ActorSystem` class provides several methods for accessing diagnostics information about local processes:

### Getting Process Information

```kotlin
// Get information about a specific process
val processInfo = system.getProcessInfo(pid)

// Get information about all processes matching a pattern
val processInfos = system.getProcessInfos("test-actor", MatchType.MATCH_PART_OF_STRING)

// Get information about all processes in the system
val allProcessInfos = system.getAllProcessInfos()
```

The `ProcessInfo` class contains detailed information about a process:

```kotlin
data class ProcessInfo(
    val pid: PID,                          // The PID of the process
    val mailboxType: String,               // The type of mailbox used by the process
    val messageCount: Int,                 // The number of messages in the mailbox
    val status: ProcessStatus,             // The status of the process (ALIVE, STOPPING, STOPPED, UNKNOWN)
    val actorType: String,                 // The type of actor
    val createdAt: Instant,                // When the process was created
    val lastMessageReceivedAt: Instant?,   // When the process last received a message
    val lastMessageSentAt: Instant?,       // When the process last sent a message
    val parent: PID?,                      // The parent of the process
    val children: List<PID>                // The children of the process
)
```

You can convert a `ProcessInfo` to a human-readable string using the `toDetailedString()` extension function:

```kotlin
val processInfo = system.getProcessInfo(pid)
val detailedString = processInfo.toDetailedString()
println(detailedString)
```

### Listing Processes

```kotlin
// List all processes in the system
val allProcesses = system.getAllProcesses()

// List processes matching a pattern
val testActors = system.listProcesses("test", MatchType.MATCH_PART_OF_STRING)

// List processes matching an exact name
val exactActors = system.listProcesses("test-actor-1", MatchType.MATCH_EXACT_STRING)

// List processes matching a regex
val regexActors = system.listProcesses(".*-actor-[12]", MatchType.MATCH_REGEX)
```

## Remote Diagnostics

ProtoActor-Kotlin also provides a remote diagnostics API for accessing diagnostics information from remote systems:

### Creating a DiagnosticsClient

```kotlin
// Create a diagnostics client for a remote system
val client = Remote.createDiagnosticsClient("localhost:8090")
```

### Getting Remote Process Information

```kotlin
// Get diagnostics information for a remote process
val diagnosticsString = client.getProcessDiagnostics(remotePid)

// Get diagnostics information asynchronously
val future = client.getProcessDiagnosticsAsync(remotePid)
future.thenAccept { diagnosticsString ->
    println(diagnosticsString)
}
```

### Listing Remote Processes

```kotlin
// List processes on the remote system matching a pattern
val remotePids = client.listProcesses("test", MatchType.MATCH_PART_OF_STRING)

// List processes asynchronously
val future = client.listProcessesAsync("test", MatchType.MATCH_PART_OF_STRING)
future.thenAccept { pids ->
    pids.forEach { println(it) }
}

// List all processes on the remote system
val allRemotePids = client.listAllProcesses()

// List all processes asynchronously
val future = client.listAllProcessesAsync()
future.thenAccept { pids ->
    pids.forEach { println(it) }
}
```

## Example: Monitoring Actor System Health

Here's an example of how to use the diagnostics API to monitor the health of your actor system:

```kotlin
import actor.proto.ActorSystem
import actor.proto.diagnostics.MatchType
import actor.proto.diagnostics.ProcessStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val system = ActorSystem("monitor")
    
    // Monitor the system every 5 seconds
    while (true) {
        val allProcessInfos = system.getAllProcessInfos()
        
        // Count processes by status
        val aliveCount = allProcessInfos.count { it.status == ProcessStatus.ALIVE }
        val stoppingCount = allProcessInfos.count { it.status == ProcessStatus.STOPPING }
        val stoppedCount = allProcessInfos.count { it.status == ProcessStatus.STOPPED }
        val unknownCount = allProcessInfos.count { it.status == ProcessStatus.UNKNOWN }
        
        // Print summary
        println("=== Actor System Health ===")
        println("Total processes: ${allProcessInfos.size}")
        println("Alive: $aliveCount")
        println("Stopping: $stoppingCount")
        println("Stopped: $stoppedCount")
        println("Unknown: $unknownCount")
        
        // Find processes with high message counts
        val highLoadProcesses = allProcessInfos.filter { it.messageCount > 100 }
        if (highLoadProcesses.isNotEmpty()) {
            println("\nProcesses with high message counts:")
            highLoadProcesses.forEach { processInfo ->
                println("${processInfo.pid}: ${processInfo.messageCount} messages")
            }
        }
        
        delay(5000) // Wait 5 seconds
    }
}
```

## Example: Remote Monitoring

Here's an example of how to use the remote diagnostics API to monitor a remote actor system:

```kotlin
import actor.proto.remote.Remote
import actor.proto.diagnostics.MatchType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create a diagnostics client for the remote system
    val client = Remote.createDiagnosticsClient("localhost:8090")
    
    // Monitor the remote system every 5 seconds
    while (true) {
        // List all processes on the remote system
        val remotePids = client.listAllProcesses()
        
        println("=== Remote Actor System Health ===")
        println("Total processes: ${remotePids.size}")
        
        // Get detailed information for each process
        for (pid in remotePids) {
            val diagnosticsString = client.getProcessDiagnostics(pid)
            println("\n$diagnosticsString")
        }
        
        delay(5000) // Wait 5 seconds
    }
}
```
