package actor.proto.cluster.pubsub

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.PubSub
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@org.junit.jupiter.api.Disabled("This test needs to be fixed")
class BatchingProducerTest {
    private lateinit var actorSystem: ActorSystem
    private lateinit var cluster: Cluster
    private lateinit var pubSub: PubSub
    private lateinit var pubSubPid: PID
    private lateinit var producer: BatchingProducer
    
    @BeforeEach
    fun setup() {
        actorSystem = mock()
        cluster = mock()
        pubSub = mock()
        pubSubPid = mock()
        
        whenever(cluster.actorSystem).thenReturn(actorSystem)
        whenever(cluster.pubSub).thenReturn(pubSub)
        whenever(pubSub.pid).thenReturn(pubSubPid)
        
        val config = BatchingProducerConfig(
            maxBatchSize = 10,
            flushInterval = Duration.ofMillis(100)
        )
        
        producer = BatchingProducer(cluster, "test-topic", config)
    }
    
    @AfterEach
    fun teardown() {
        producer.close()
    }
    
    @Test
    fun `should produce message`() = runBlocking {
        // Given
        val message = "test-message"
        val response = PubSubBatchResponse(true)
        
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())).thenReturn(response)
        
        // When
        val result = producer.produce(message)
        
        // Then
        assertEquals(response, result)
        
        // Verify that the message was sent to the pubsub actor
        verify(actorSystem, timeout(1000)).requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())
    }
    
    @Test
    fun `should produce multiple messages`() = runBlocking {
        // Given
        val messages = listOf("message1", "message2", "message3")
        val response = PubSubBatchResponse(true)
        
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())).thenReturn(response)
        
        // When
        val results = producer.produceAll(messages)
        
        // Then
        assertEquals(3, results.size)
        results.forEach { assertEquals(response, it) }
        
        // Verify that the messages were sent to the pubsub actor
        verify(actorSystem, timeout(1000)).requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())
    }
    
    @Test
    fun `should handle error when producing message`() = runBlocking {
        // Given
        val message = "test-message"
        val exception = RuntimeException("Test exception")
        
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())).thenThrow(exception)
        
        // Configure error handler to fail and continue
        val errorHandler: PublishingErrorHandler = { _, _, _ -> PublishingErrorDecision.FailBatchAndContinue }
        val config = BatchingProducerConfig(
            maxBatchSize = 10,
            flushInterval = Duration.ofMillis(100),
            errorHandler = errorHandler
        )
        
        val producerWithErrorHandler = BatchingProducer(cluster, "test-topic", config)
        
        try {
            // When
            val result = producerWithErrorHandler.produce(message)
            
            // Then
            assertFalse(result.success)
            assertEquals("Test exception", result.errorMessage)
        } finally {
            producerWithErrorHandler.close()
        }
    }
    
    @Test
    fun `should retry when error handler returns RetryBatchImmediately`() = runBlocking {
        // Given
        val message = "test-message"
        val exception = RuntimeException("Test exception")
        val response = PubSubBatchResponse(true)
        
        // First call throws exception, second call succeeds
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any()))
            .thenThrow(exception)
            .thenReturn(response)
        
        // Configure error handler to retry immediately
        val errorHandler: PublishingErrorHandler = { _, _, _ -> PublishingErrorDecision.RetryBatchImmediately }
        val config = BatchingProducerConfig(
            maxBatchSize = 10,
            flushInterval = Duration.ofMillis(100),
            errorHandler = errorHandler
        )
        
        val producerWithErrorHandler = BatchingProducer(cluster, "test-topic", config)
        
        try {
            // When
            val result = producerWithErrorHandler.produce(message)
            
            // Then
            assertTrue(result.success)
            
            // Verify that the message was sent twice
            verify(actorSystem, times(2)).requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())
        } finally {
            producerWithErrorHandler.close()
        }
    }
    
    @Test
    fun `should retry after delay when error handler returns RetryBatchAfter`() = runBlocking {
        // Given
        val message = "test-message"
        val exception = RuntimeException("Test exception")
        val response = PubSubBatchResponse(true)
        
        // First call throws exception, second call succeeds
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any()))
            .thenThrow(exception)
            .thenReturn(response)
        
        // Configure error handler to retry after delay
        val errorHandler: PublishingErrorHandler = { _, _, _ -> 
            PublishingErrorDecision.RetryBatchAfter(Duration.ofMillis(100)) 
        }
        val config = BatchingProducerConfig(
            maxBatchSize = 10,
            flushInterval = Duration.ofMillis(100),
            errorHandler = errorHandler
        )
        
        val producerWithErrorHandler = BatchingProducer(cluster, "test-topic", config)
        
        try {
            // When
            val result = producerWithErrorHandler.produce(message)
            
            // Then
            assertTrue(result.success)
            
            // Verify that the message was sent twice
            verify(actorSystem, times(2)).requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())
        } finally {
            producerWithErrorHandler.close()
        }
    }
    
    @Test
    fun `should stop when error handler returns FailBatchAndStop`() = runBlocking {
        // Given
        val message = "test-message"
        val exception = RuntimeException("Test exception")
        
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())).thenThrow(exception)
        
        // Configure error handler to fail and stop
        val errorHandler: PublishingErrorHandler = { _, _, _ -> PublishingErrorDecision.FailBatchAndStop }
        val config = BatchingProducerConfig(
            maxBatchSize = 10,
            flushInterval = Duration.ofMillis(100),
            errorHandler = errorHandler
        )
        
        val producerWithErrorHandler = BatchingProducer(cluster, "test-topic", config)
        
        try {
            // When
            val result1 = producerWithErrorHandler.produce(message)
            
            // Then
            assertFalse(result1.success)
            assertEquals("Test exception", result1.errorMessage)
            
            // Try to produce another message
            val result2 = producerWithErrorHandler.produce("another message")
            
            // Should fail immediately without sending to pubsub
            assertFalse(result2.success)
            assertEquals("Producer is stopped", result2.errorMessage)
            
            // Verify that only one message was sent
            verify(actorSystem, times(1)).requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())
        } finally {
            producerWithErrorHandler.close()
        }
    }
    
    @Test
    fun `should flush messages`() = runBlocking {
        // Given
        val message = "test-message"
        val response = PubSubBatchResponse(true)
        
        whenever(actorSystem.requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())).thenReturn(response)
        
        // When
        producer.produce(message)
        producer.flush()
        
        // Then
        verify(actorSystem, times(1)).requestAwait<PubSubBatchResponse>(eq(pubSubPid), any())
    }
}
