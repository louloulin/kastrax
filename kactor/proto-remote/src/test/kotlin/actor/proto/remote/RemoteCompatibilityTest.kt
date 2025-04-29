package actor.proto.remote

import actor.proto.PID
import actor.proto.fromProducer
import actor.proto.send
import actor.proto.spawn
import actor.proto.stop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * This test verifies the compatibility with ProtoActor-Go.
 * It requires a running ProtoActor-Go server to work.
 * 
 * To run the test, start the ProtoActor-Go server first:
 * ```
 * cd protoactor-go/examples/remoterouting
 * go run server/main.go
 * ```
 */
@Disabled("Requires a running ProtoActor-Go server")
class RemoteCompatibilityTest {
    
    @BeforeEach
    fun setup() {
        // Start the remote server
        Remote.start("localhost", 8090)
        
        // Register the test actor
        Remote.registerKnownKind("echo", fromProducer { EchoActor() })
    }
    
    @AfterEach
    fun teardown() {
        // Stop the actors
        stop(Remote.endpointManagerPid)
        stop(Remote.activatorPid)
    }
    
    @Test
    fun `should communicate with ProtoActor-Go server`() {
        // Create a future to wait for the response
        val future = CompletableFuture<String>()
        
        // Create a receiver actor
        val receiverProps = fromProducer { ReceiverActor(future) }
        val receiver = spawn(receiverProps)
        
        // Create a remote actor on the Go server
        val remotePid = PID("localhost:12000", "echo")
        
        // Send a message to the remote actor
        send(remotePid, EchoRequest("Hello from Kotlin", receiver))
        
        // Wait for the response
        val response = future.get(5, TimeUnit.SECONDS)
        
        // Verify the response
        assert(response == "Hello from Kotlin") { "Expected 'Hello from Kotlin', got '$response'" }
        
        // Stop the receiver
        stop(receiver)
    }
    
    class EchoActor : actor.proto.Actor {
        override suspend fun actor.proto.Context.receive(msg: Any) {
            when (msg) {
                is EchoRequest -> {
                    // Echo the message back to the sender
                    send(msg.sender, EchoResponse(msg.message))
                }
            }
        }
    }
    
    class ReceiverActor(private val future: CompletableFuture<String>) : actor.proto.Actor {
        override suspend fun actor.proto.Context.receive(msg: Any) {
            when (msg) {
                is EchoResponse -> {
                    // Complete the future with the response
                    future.complete(msg.message)
                }
            }
        }
    }
    
    data class EchoRequest(val message: String, val sender: PID)
    data class EchoResponse(val message: String)
}
