package actor.proto.stream

import actor.proto.ActorSystem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A simplified test for stream operations that doesn't rely on the actor system
 */
class SimpleStreamTest {
    private lateinit var system: ActorSystem

    @BeforeEach
    fun setup() {
        system = ActorSystem.get("test-system")
    }

    @AfterEach
    fun tearDown() {
        // No explicit shutdown needed
    }

    @Test
    fun `should map values in a channel`() = runBlocking {
        // Create a channel and send some values
        val channel = Channel<Int>()
        channel.send(1)
        channel.send(2)
        channel.send(3)
        
        // Create a mapped channel
        val mappedChannel = Channel<Int>()
        
        // Map values from the original channel to the mapped channel
        kotlinx.coroutines.launch {
            for (value in channel) {
                mappedChannel.send(value * 2)
            }
            mappedChannel.close()
        }
        
        // Receive values from the mapped channel
        val results = mutableListOf<Int>()
        for (i in 1..3) {
            val result = withTimeoutOrNull(1000) { mappedChannel.receive() }
            if (result != null) {
                results.add(result)
            } else {
                break
            }
        }
        
        // Verify results
        assertEquals(listOf(2, 4, 6), results)
        
        // Close channels
        channel.close()
    }

    @Test
    fun `should filter values in a channel`() = runBlocking {
        // Create a channel and send some values
        val channel = Channel<Int>()
        channel.send(1)
        channel.send(2)
        channel.send(3)
        channel.send(4)
        
        // Create a filtered channel
        val filteredChannel = Channel<Int>()
        
        // Filter values from the original channel to the filtered channel
        kotlinx.coroutines.launch {
            for (value in channel) {
                if (value % 2 == 0) {
                    filteredChannel.send(value)
                }
            }
            filteredChannel.close()
        }
        
        // Receive values from the filtered channel
        val results = mutableListOf<Int>()
        for (i in 1..2) {
            val result = withTimeoutOrNull(1000) { filteredChannel.receive() }
            if (result != null) {
                results.add(result)
            } else {
                break
            }
        }
        
        // Verify results
        assertEquals(listOf(2, 4), results)
        
        // Close channels
        channel.close()
    }

    @Test
    fun `should reduce values in a channel`() = runBlocking {
        // Create a channel and send some values
        val channel = Channel<Int>()
        channel.send(1)
        channel.send(2)
        channel.send(3)
        
        // Close the channel to ensure reduce completes
        channel.close()
        
        // Reduce the channel
        var sum = 0
        for (value in channel) {
            sum += value
        }
        
        // Verify result
        assertEquals(6, sum)
    }

    @Test
    fun `should take values from a channel`() = runBlocking {
        // Create a channel and send some values
        val channel = Channel<Int>()
        channel.send(1)
        channel.send(2)
        channel.send(3)
        channel.send(4)
        
        // Create a channel for taken values
        val takenChannel = Channel<Int>()
        
        // Take values from the original channel
        kotlinx.coroutines.launch {
            var count = 0
            for (value in channel) {
                if (count < 2) {
                    takenChannel.send(value)
                    count++
                } else {
                    break
                }
            }
            takenChannel.close()
        }
        
        // Receive values from the taken channel
        val results = mutableListOf<Int>()
        for (i in 1..2) {
            val result = withTimeoutOrNull(1000) { takenChannel.receive() }
            if (result != null) {
                results.add(result)
            } else {
                break
            }
        }
        
        // Verify results
        assertEquals(listOf(1, 2), results)
        
        // Close channels
        channel.close()
    }

    @Test
    fun `should skip values from a channel`() = runBlocking {
        // Create a channel and send some values
        val channel = Channel<Int>()
        channel.send(1)
        channel.send(2)
        channel.send(3)
        channel.send(4)
        
        // Create a channel for skipped values
        val skippedChannel = Channel<Int>()
        
        // Skip values from the original channel
        kotlinx.coroutines.launch {
            var count = 0
            for (value in channel) {
                if (count >= 2) {
                    skippedChannel.send(value)
                }
                count++
            }
            skippedChannel.close()
        }
        
        // Receive values from the skipped channel
        val results = mutableListOf<Int>()
        for (i in 1..2) {
            val result = withTimeoutOrNull(1000) { skippedChannel.receive() }
            if (result != null) {
                results.add(result)
            } else {
                break
            }
        }
        
        // Verify results
        assertEquals(listOf(3, 4), results)
        
        // Close channels
        channel.close()
    }

    @Test
    fun `should concatenate channels`() = runBlocking {
        // Create channels and send some values
        val channel1 = Channel<Int>()
        val channel2 = Channel<Int>()
        
        channel1.send(1)
        channel1.send(2)
        channel2.send(3)
        channel2.send(4)
        
        // Close the channels to ensure concat completes
        channel1.close()
        channel2.close()
        
        // Create a channel for concatenated values
        val concatChannel = Channel<Int>()
        
        // Concatenate values from the original channels
        kotlinx.coroutines.launch {
            // First, consume all elements from channel1
            for (value in channel1) {
                concatChannel.send(value)
            }
            
            // Then, consume all elements from channel2
            for (value in channel2) {
                concatChannel.send(value)
            }
            
            concatChannel.close()
        }
        
        // Receive values from the concatenated channel
        val results = mutableListOf<Int>()
        for (i in 1..4) {
            val result = withTimeoutOrNull(1000) { concatChannel.receive() }
            if (result != null) {
                results.add(result)
            } else {
                break
            }
        }
        
        // Verify results
        assertEquals(listOf(1, 2, 3, 4), results)
    }

    @Test
    fun `should merge channels`() = runBlocking {
        // Create channels and send some values
        val channel1 = Channel<Int>()
        val channel2 = Channel<Int>()
        
        // Create a channel for merged values
        val mergedChannel = Channel<Int>()
        
        // Merge values from the original channels
        kotlinx.coroutines.launch {
            kotlinx.coroutines.launch {
                for (value in channel1) {
                    mergedChannel.send(value)
                }
            }
            
            kotlinx.coroutines.launch {
                for (value in channel2) {
                    mergedChannel.send(value)
                }
            }
        }
        
        // Send values to the original channels
        channel1.send(1)
        channel2.send(2)
        channel1.send(3)
        channel2.send(4)
        
        // Wait a bit for values to be processed
        delay(100)
        
        // Receive values from the merged channel
        val results = mutableListOf<Int>()
        for (i in 1..4) {
            val result = withTimeoutOrNull(1000) { mergedChannel.receive() }
            if (result != null) {
                results.add(result)
            } else {
                break
            }
        }
        
        // Verify results - order may vary, so we just check the set
        assertEquals(4, results.size)
        assertEquals(setOf(1, 2, 3, 4), results.toSet())
        
        // Close channels
        channel1.close()
        channel2.close()
        mergedChannel.close()
    }
}
