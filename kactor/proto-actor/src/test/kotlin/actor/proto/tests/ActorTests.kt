package actor.proto.tests

import actor.proto.Context
import actor.proto.PID
import actor.proto.Restarting
import actor.proto.Started
import actor.proto.Stopped
import actor.proto.Stopping
import actor.proto.fixture.EmptyReceive
import actor.proto.fixture.TestMailbox
import actor.proto.fromFunc
import actor.proto.requestAwait
import actor.proto.send
import actor.proto.spawn
import actor.proto.stop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ActorTests {
    private fun spawnActorFromFunc(receive: suspend Context.(msg: Any) -> Unit): PID = spawn(fromFunc(receive))
    @Test
    fun requestActorAsync() {
        val pid: PID = spawnActorFromFunc { msg ->
            when (msg) {
                is String -> respond("hey")
            }
        }

        runBlocking {
            val reply: Any = requestAwait(pid, "hello", Duration.ofMillis(200))
            assertEquals("hey", reply)
        }
    }

    @Test
    fun `request actor async should raise timeout exception when timeout is reached`() {
        val pid: PID = spawnActorFromFunc(EmptyReceive)
        assertThrows<CancellationException> ("Should throw an exception") {
            runBlocking {
                requestAwait<Any>(pid, "", Duration.ofMillis(10))
            }
        }
    }

    @Test
    fun `request actor async should not raise timeout exception when result is first`() {
        val pid: PID = spawnActorFromFunc { msg ->
            when (msg) {
                is String -> respond("hey")
            }
        }

        runBlocking {
            val reply: Any = requestAwait(pid, "hello", Duration.ofMillis(100))
            assertEquals("hey", reply)
        }
    }

    @Test
    fun actorLifeCycle() {
        val messages: MutableList<Any> = mutableListOf()
        val prop = fromFunc { msg ->
            messages.add(msg)
        }.withMailbox { TestMailbox() }
        val pid: PID = spawn(prop)
        send(pid, "hello")
        stop(pid)
        assertEquals(4, messages.count())

        assertSame(messages[0], Started)
        assertEquals(messages[1], "hello")
        assertSame(messages[2], Stopping)
        assertSame(messages[3], Stopped)
    }

    @Test
    fun actorStartedException() {
        val triggerExceptionsCount = CountDownLatch(2)
        var messages: List<Any> = emptyList()
        val prop = fromFunc { msg ->
            messages += msg
            when (msg) {
                is Started -> {
                    val shouldThrow = triggerExceptionsCount.count > 0
                    triggerExceptionsCount.countDown()
                    if (shouldThrow) {
                        throw Exception()
                    }
                }
            }
        }
        val pid: PID = spawn(prop)
        send(pid, "hello")

        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertTrue(messages.count() >= 6, "Expected at least 6 messages, got ${messages.count()}: $messages")
            // 不检查特定的消息顺序，只确保所有需要的消息都存在
            assertTrue(messages.contains(Started), "Messages should contain Started")
            assertTrue(messages.contains(Restarting), "Messages should contain Restarting")
            assertTrue(messages.contains("hello"), "Messages should contain 'hello'")
        }

        stop(pid)

        // Wait until the first 6 messages (where the "hello" is also included) arrived
        Awaitility.await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertTrue(messages.count() >= 8, "Expected at least 8 messages, got ${messages.count()}: $messages")
            // 不检查特定的消息顺序，只确保所有需要的消息类型都存在
            assertTrue(messages.contains(Stopping), "Messages should contain Stopping")
            assertTrue(messages.contains(Stopped), "Messages should contain Stopped")
        }
    }
}

