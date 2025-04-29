package actor.proto.examples.messagebatch

import actor.proto.*
import actor.proto.mailbox.MessageBatch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Simple Kotlin example demonstrating the use of MessageBatch.
 */
object KotlinBatchDemo {
    // Define a simple message class
    data class SimpleMessage(val text: String)
    
    // Define a message batch implementation
    class SimpleMessageBatch(private val messages: List<Any>) : MessageBatch {
        override fun getMessages(): List<Any> = messages
    }
    
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
                is SimpleMessage -> {
                    println("Received individual message: ${msg.text}")
                    messagesLatch.countDown()
                }
                is SimpleMessageBatch -> {
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
        val messages = List(messageCount) { SimpleMessage("Message $it") }
        val batch = SimpleMessageBatch(messages)
        
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
