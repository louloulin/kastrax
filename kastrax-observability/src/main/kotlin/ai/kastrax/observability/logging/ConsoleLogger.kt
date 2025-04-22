package ai.kastrax.observability.logging

import java.text.SimpleDateFormat
import java.util.Date

/**
 * 控制台日志记录器工厂。
 */
class ConsoleLoggerFactory : LoggerFactory {
    private val loggers = mutableMapOf<String, Logger>()
    private var minLevel = LogLevel.INFO

    /**
     * 设置最小日志级别。
     *
     * @param level 日志级别
     */
    fun setMinLevel(level: LogLevel) {
        minLevel = level
    }

    /**
     * 获取最小日志级别。
     *
     * @return 日志级别
     */
    fun getMinLevel(): LogLevel {
        return minLevel
    }

    override fun getLogger(name: String): Logger {
        return loggers.getOrPut(name) { ConsoleLogger(name, this) }
    }
}

/**
 * 控制台日志记录器。
 *
 * @property name 日志记录器名称
 * @property factory 日志记录器工厂
 */
class ConsoleLogger(
    private val name: String,
    private val factory: ConsoleLoggerFactory
) : Logger {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    override fun getName(): String {
        return name
    }

    override fun isEnabled(level: LogLevel): Boolean {
        return level.ordinal >= factory.getMinLevel().ordinal
    }

    override fun log(entry: LogEntry) {
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val levelStr = entry.level.name.padEnd(5)
        val contextStr = if (entry.context.isNotEmpty()) {
            " " + entry.context.entries.joinToString(", ") { "${it.key}=${formatValue(it.value)}" }
        } else {
            ""
        }

        val message = "$timestamp [$levelStr] $name - ${entry.message}$contextStr"
        
        when (entry.level) {
            LogLevel.ERROR, LogLevel.FATAL -> System.err.println(message)
            else -> println(message)
        }

        entry.exception?.let {
            it.printStackTrace(if (entry.level in listOf(LogLevel.ERROR, LogLevel.FATAL)) System.err else System.out)
        }
    }

    private fun formatValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"$value\""
            is Array<*> -> value.contentToString()
            is Collection<*> -> value.toString()
            is Map<*, *> -> value.toString()
            else -> value.toString()
        }
    }
}
