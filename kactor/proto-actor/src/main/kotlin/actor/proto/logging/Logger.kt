package actor.proto.logging

/**
 * Logger interface for ProtoActor-Kotlin
 * Provides methods for logging at different levels
 */
interface Logger {
    /**
     * Log a message at the debug level
     * @param message The message to log
     * @param args Additional arguments to include in the log
     */
    fun debug(message: String, vararg args: Pair<String, Any?>)

    /**
     * Log a message at the info level
     * @param message The message to log
     * @param args Additional arguments to include in the log
     */
    fun info(message: String, vararg args: Pair<String, Any?>)

    /**
     * Log a message at the warning level
     * @param message The message to log
     * @param args Additional arguments to include in the log
     */
    fun warning(message: String, vararg args: Pair<String, Any?>)

    /**
     * Log a message at the error level
     * @param message The message to log
     * @param args Additional arguments to include in the log
     */
    fun error(message: String, vararg args: Pair<String, Any?>)

    /**
     * Log a message at the error level with an exception
     * @param message The message to log
     * @param exception The exception to log
     * @param args Additional arguments to include in the log
     */
    fun error(message: String, exception: Throwable, vararg args: Pair<String, Any?>)

    /**
     * Create a new logger with additional context
     * @param args Additional context to include in all log messages
     * @return A new logger with the additional context
     */
    fun withContext(vararg args: Pair<String, Any?>): Logger
}

/**
 * Factory for creating loggers
 */
interface LoggerFactory {
    /**
     * Create a new logger with the given name
     * @param name The name of the logger
     * @return A new logger
     */
    fun getLogger(name: String): Logger
}

/**
 * Log level enum
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/**
 * Configuration for logging
 */
data class LoggingConfig(
    val level: LogLevel = LogLevel.INFO,
    val format: String = "[{level}] {time} {message} {context}",
    val includeTimestamp: Boolean = true,
    val includeContext: Boolean = true
)
