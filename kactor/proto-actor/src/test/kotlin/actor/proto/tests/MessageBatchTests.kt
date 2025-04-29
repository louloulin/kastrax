package actor.proto.tests

import actor.proto.Context
import actor.proto.PID
import actor.proto.fromFunc
import actor.proto.mailbox.MessageBatch
import actor.proto.send
import actor.proto.spawn
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MessageBatchTests {

    class TestMessage(val value: String)

    class TestMessageBatch(private val messages: List<Any>) : MessageBatch {
        override fun getMessages(): List<Any> = messages
    }

    @Test
    fun `actor receives each message in a message batch`() = runBlocking {
        // Create a latch to wait for all messages to be processed
        val messageCount = 10
        val messagesLatch = CountDownLatch(messageCount)
        val batchLatch = CountDownLatch(1)

        // Create an actor that counts down the latch for each message
        val pid: PID = spawn(fromFunc { msg ->
            when (msg) {
                is TestMessage -> {
                    messagesLatch.countDown()
                }
                is TestMessageBatch -> {
                    batchLatch.countDown()
                }
            }
        })

        // Create a batch of messages
        val messages = List(messageCount) { TestMessage("Message $it") }
        val batch = TestMessageBatch(messages)

        // Send the batch to the actor
        send(pid, batch)

        // Wait for all messages to be processed
        val messagesProcessed = messagesLatch.await(5, TimeUnit.SECONDS)
        val batchProcessed = batchLatch.await(5, TimeUnit.SECONDS)

        // Assert that all messages were processed
        assertEquals(true, messagesProcessed, "All individual messages should be processed")
        assertEquals(true, batchProcessed, "The batch message itself should be processed")
    }
}
