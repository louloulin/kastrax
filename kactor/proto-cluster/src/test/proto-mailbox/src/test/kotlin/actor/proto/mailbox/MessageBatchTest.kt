package actor.proto.mailbox

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MessageBatchTest {
    
    // Define a simple message class
    data class TestMessage(val value: String)
    
    // Define a message batch implementation
    class TestMessageBatch(private val messages: List<Any>) : MessageBatch {
        override fun getMessages(): List<Any> = messages
    }
    
    @Test
    fun `mailbox should process each message in a batch`() {
        // Create a latch to wait for all messages to be processed
        val messageCount = 5
        val latch = CountDownLatch(messageCount)
        
        // Create a message invoker that counts down the latch for each message
        val invoker = object : MessageInvoker {
            override fun invokeSystemMessage(message: Any) {}
            
            override fun invokeUserMessage(message: Any) {
                if (message is TestMessage) {
                    println("Processed message: ${message.value}")
                    latch.countDown()
                }
            }
            
            override fun escalateFailure(reason: Any, message: Any) {}
        }
        
        // Create a dispatcher
        val dispatcher = object : Dispatcher {
            override val throughput: Int = 10
            
            override fun schedule(mailbox: Runnable) {
                mailbox.run()
            }
        }
        
        // Create a mailbox
        val mailbox = DefaultMailbox(ArrayDeque(), ArrayDeque())
        mailbox.registerHandlers(invoker, dispatcher)
        
        // Create a batch of messages
        val messages = List(messageCount) { TestMessage("Message $it") }
        val batch = TestMessageBatch(messages)
        
        // Post the batch to the mailbox
        mailbox.postUserMessage(batch)
        
        // Wait for all messages to be processed
        val allProcessed = latch.await(5, TimeUnit.SECONDS)
        
        // Assert that all messages were processed
        assertEquals(true, allProcessed, "All messages in the batch should be processed")
    }
}
