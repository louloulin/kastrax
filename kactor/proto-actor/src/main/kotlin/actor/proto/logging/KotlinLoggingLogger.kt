package actor.proto.logging

import mu.KLogger
import mu.KotlinLogging

/**
 * Implementation of Logger using kotlin-logging
 */
class KotlinLoggingLogger(
    private val logger: KLogger,
    private val context: Map<String, Any?> = emptyMap()
) : Logger {
    override fun debug(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isDebugEnabled) {
            val formattedMessage = formatMessage(message, args)
            logger.debug { formattedMessage }
        }
    }

    override fun info(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isInfoEnabled) {
            val formattedMessage = formatMessage(message, args)
            logger.info { formattedMessage }
        }
    }

    override fun warning(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isWarnEnabled) {
            val formattedMessage = formatMessage(message, args)
            logger.warn { formattedMessage }
        }
    }

    override fun error(message: String, vararg args: Pair<String, Any?>) {
        if (logger.isErrorEnabled) {
            val formattedMessage = formatMessage(message, args)
            logger.error { formattedMessage }
        }
    }

    override fun error(message: String, exception: Throwable, vararg args: Pair<String, Any?>) {
        if (logger.isErrorEnabled) {
            val formattedMessage = formatMessage(message, args)
            logger.error(exception) { formattedMessage }
        }
    }

    override fun withContext(vararg args: Pair<String, Any?>): Logger {
        val newContext = context.toMutableMap()
        args.forEach { newContext[it.first] = it.second }
        return KotlinLoggingLogger(logger, newContext)
    }

    private fun formatMessage(message: String, args: Array<out Pair<String, Any?>>): String {
        val allContext = context.toMutableMap()
        args.forEach { allContext[it.first] = it.second }
        
        return if (allContext.isEmpty()) {
            message
        } else {
            val contextString = allContext.entries.joinToString(", ") { "${it.key}=${formatValue(it.value)}" }
            "$message | $contextString"
        }
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Array<*> -> value.joinToString(", ", "[", "]") { formatValue(it) }
            is Collection<*> -> value.joinToString(", ", "[", "]") { formatValue(it) }
            is Map<*, *> -> value.entries.joinToString(", ", "{", "}") { "${formatValue(it.key)}=${formatValue(it.value)}" }
            else -> value.toString()
        }
    }
}

/**
 * Factory for creating KotlinLoggingLogger instances
 */
class KotlinLoggingLoggerFactory : LoggerFactory {
    override fun getLogger(name: String): Logger {
        return KotlinLoggingLogger(KotlinLogging.logger(name))
    }
}

/**
 * Default logger factory
 */
object DefaultLoggerFactory {
    private val factory = KotlinLoggingLoggerFactory()
    
    fun getLogger(name: String): Logger {
        return factory.getLogger(name)
    }
}
