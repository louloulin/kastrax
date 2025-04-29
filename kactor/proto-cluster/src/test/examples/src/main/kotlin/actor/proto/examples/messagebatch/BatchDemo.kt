package actor.proto.examples.messagebatch

import actor.proto.*
import actor.proto.mailbox.MessageBatch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Define a simple message class
data class DemoMessage(val text: String)

// Define a message batch implementation
class DemoBatch(private val messages: List<Any>) : MessageBatch {
    override fun getMessages(): List<Any> = messages
}

/**
 * Simple standalone example demonstrating the use of MessageBatch.
 */
object BatchDemo {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Starting MessageBatch demo...")
        
        // Create a latch to wait for all messages to be processed
        val messageCount = 5
        val messagesLatch = CountDownLatch(messageCount)
        val batchLatch = CountDownLatch(1)
        
        // Create an actor that counts down the latch for each message
        val props = fromFunc { msg ->
            when (msg) {
                is DemoMessage -> {
                    println("Received individual message: ${msg.text}")
                    messagesLatch.countDown()
                }
                is DemoBatch -> {
                    println("Received batch with ${msg.getMessages().size} messages")
                    batchLatch.countDown()
                }
                else -> {
                    println("Received unknown message: $msg")
                }
            }
        }
        
        // Spawn the actor
        val pid = spawn(props)
        
        // Create a batch of messages
        val messages = List(messageCount) { DemoMessage("Message $it") }
        val batch = DemoBatch(messages)
        
        println("Sending a batch of ${messages.size} messages...")
        
        // Send the batch to the actor
        send(pid, batch)
        
        // Wait for all messages to be processed
        val messagesProcessed = messagesLatch.await(5, TimeUnit.SECONDS)
        val batchProcessed = batchLatch.await(5, TimeUnit.SECONDS)
        
        // Print results
        println("\nResults:")
        println("All individual messages processed: $messagesProcessed")
        println("Batch message itself processed: $batchProcessed")
        
        // Stop the actor
        stop(pid)
        
        println("MessageBatch demo completed.")
    }
}
