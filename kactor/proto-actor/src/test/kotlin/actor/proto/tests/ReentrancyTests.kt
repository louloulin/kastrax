package actor.proto.tests

import actor.proto.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ReentrancyTests {
    
    @Test
    fun `given actor when reenterAfter should continue processing with future result`() {
        val system = ActorSystem("test-system")
        val latch = CountDownLatch(1)
        var result = ""
        
        // Create an actor that will respond with "pong"
        val pongActor = system.actorOf(fromFunc { msg ->
            if (msg is String && msg == "ping") {
                respond("pong")
            }
        })
        
        // Create an actor that will use reenterAfter to process the response
        val reenterActor = system.actorOf(fromFunc { msg ->
            when (msg) {
                is String -> {
                    if (msg == "start") {
                        val future = requestFuture<String>(pongActor, "ping", Duration.ofSeconds(5))
                        reenterAfter(future) { res, err ->
                            if (err == null && res != null) {
                                result = "received: $res"
                                latch.countDown()
                            }
                        }
                    }
                }
            }
        })
        
        // Start the test
        system.send(reenterActor, "start")
        
        // Wait for the test to complete
        latch.await(5, TimeUnit.SECONDS)
        
        // Verify the result
        assertEquals("received: pong", result)
    }
    
    @Test
    fun `given actor when reenterAfter with chain should process all results`() {
        val system = ActorSystem("test-system")
        val latch = CountDownLatch(1)
        var result = ""
        
        // Create actors that will respond with different messages
        val actor1 = system.actorOf(fromFunc { msg ->
            if (msg is String && msg == "request1") {
                respond("response1")
            }
        })
        
        val actor2 = system.actorOf(fromFunc { msg ->
            if (msg is String && msg == "request2") {
                respond("response2")
            }
        })
        
        // Create an actor that will use reenterAfter to chain requests
        val reenterActor = system.actorOf(fromFunc { msg ->
            when (msg) {
                is String -> {
                    if (msg == "start") {
                        val future1 = requestFuture<String>(actor1, "request1", Duration.ofSeconds(5))
                        reenterAfter(future1) { res1, err1 ->
                            if (err1 == null && res1 != null) {
                                val future2 = requestFuture<String>(actor2, "request2", Duration.ofSeconds(5))
                                reenterAfter(future2) { res2, err2 ->
                                    if (err2 == null && res2 != null) {
                                        result = "$res1 + $res2"
                                        latch.countDown()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
        
        // Start the test
        system.send(reenterActor, "start")
        
        // Wait for the test to complete
        latch.await(5, TimeUnit.SECONDS)
        
        // Verify the result
        assertEquals("response1 + response2", result)
    }
}
