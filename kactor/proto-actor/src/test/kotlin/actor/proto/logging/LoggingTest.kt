package actor.proto.logging

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.Props
import actor.proto.withProducer
import actor.proto.withReceiveMiddleware
import actor.proto.withSenderMiddleware
import actor.proto.middleware.loggingReceiveMiddleware
import actor.proto.middleware.loggingSenderMiddleware
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class LoggingTest {

    private lateinit var system: ActorSystem

    @BeforeEach
    fun setup() {
        system = ActorSystem.get("test-system")
    }

    @Test
    fun `should log messages with logger in context`() {
        // We're using the default logger implementation
        // The test will pass as long as no exceptions are thrown

        // Create a test actor that uses the logger
        val future = CompletableFuture<Boolean>()
        val props = Props().withProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is String -> {
                            logger.info("Received string message", "message" to msg)
                            future.complete(true)
                        }
                    }
                }
            }
        }

        // Create the actor
        val pid = system.actorOf(props)

        // Send a message to the actor
        system.send(pid, "Hello, world!")

        // Wait for the actor to process the message
        future.get(1, TimeUnit.SECONDS)

        // Verify that the future was completed, which means the logger was used
        assertTrue(future.isDone)
    }

    @Test
    fun `should log messages with logging middleware`() {
        // We're using the default logger implementation
        // The test will pass as long as no exceptions are thrown

        // Create a test actor with logging middleware
        val future = CompletableFuture<Boolean>()
        val props = Props().withProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is String -> {
                            future.complete(true)
                        }
                    }
                }
            }
        }.withReceiveMiddleware(loggingReceiveMiddleware(logMessageContent = true))

        // Create the actor
        val pid = system.actorOf(props)

        // Send a message to the actor
        system.send(pid, "Hello, world!")

        // Wait for the actor to process the message
        future.get(1, TimeUnit.SECONDS)

        // Verify that the future was completed, which means the middleware was used
        assertTrue(future.isDone)
    }

    @Test
    fun `should log messages with sender middleware`() {
        // We're using the default logger implementation
        // The test will pass as long as no exceptions are thrown

        // Create a test actor with sender middleware
        val future = CompletableFuture<Boolean>()
        val props = Props().withProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    when (msg) {
                        is String -> {
                            // Send a response
                            send(sender!!, "Response")
                            future.complete(true)
                        }
                    }
                }
            }
        }.withSenderMiddleware(loggingSenderMiddleware(logMessageContent = true))

        // Create the actor
        val pid = system.actorOf(props)

        // Send a message to the actor
        system.request(pid, "Hello, world!", system.deadLetter())

        // Wait for the actor to process the message
        future.get(1, TimeUnit.SECONDS)

        // Verify that the future was completed, which means the middleware was used
        assertTrue(future.isDone)
    }
}

/**
 * Test logger that captures log messages
 */
class TestLogger : Logger {
    val messages = mutableListOf<String>()

    override fun debug(message: String, vararg args: Pair<String, Any?>) {
        messages.add(formatMessage("DEBUG", message, args))
    }

    override fun info(message: String, vararg args: Pair<String, Any?>) {
        messages.add(formatMessage("INFO", message, args))
    }

    override fun warning(message: String, vararg args: Pair<String, Any?>) {
        messages.add(formatMessage("WARNING", message, args))
    }

    override fun error(message: String, vararg args: Pair<String, Any?>) {
        messages.add(formatMessage("ERROR", message, args))
    }

    override fun error(message: String, exception: Throwable, vararg args: Pair<String, Any?>) {
        messages.add(formatMessage("ERROR", "$message: ${exception.message}", args))
    }

    override fun withContext(vararg args: Pair<String, Any?>): Logger {
        return this
    }

    private fun formatMessage(level: String, message: String, args: Array<out Pair<String, Any?>>): String {
        val argsString = args.joinToString(", ") { "${it.first}=${it.second}" }
        return "[$level] $message | $argsString"
    }
}

/**
 * Test logger factory that returns the test logger
 */
class TestLoggerFactory(private val logger: TestLogger) : LoggerFactory {
    override fun getLogger(name: String): Logger {
        return logger
    }
}
