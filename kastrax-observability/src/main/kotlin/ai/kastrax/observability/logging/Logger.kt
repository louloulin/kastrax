package ai.kastrax.observability.logging

/**
 * 日志级别枚举。
 */
enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FATAL
}

/**
 * 结构化日志条目。
 *
 * @property timestamp 日志时间戳
 * @property level 日志级别
 * @property message 日志消息
 * @property exception 异常信息（可选）
 * @property context 上下文信息
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val message: String,
    val exception: Throwable? = null,
    val context: Map<String, Any?> = emptyMap()
)

/**
 * 日志接口。
 */
interface Logger {
    /**
     * 获取日志名称。
     *
     * @return 日志名称
     */
    fun getName(): String

    /**
     * 检查指定日志级别是否启用。
     *
     * @param level 日志级别
     * @return 如果启用则返回 true，否则返回 false
     */
    fun isEnabled(level: LogLevel): Boolean

    /**
     * 记录日志。
     *
     * @param entry 日志条目
     */
    fun log(entry: LogEntry)

    /**
     * 记录 TRACE 级别日志。
     *
     * @param message 日志消息
     * @param context 上下文信息
     */
    fun trace(message: String, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.TRACE)) {
            log(LogEntry(level = LogLevel.TRACE, message = message, context = context))
        }
    }

    /**
     * 记录 DEBUG 级别日志。
     *
     * @param message 日志消息
     * @param context 上下文信息
     */
    fun debug(message: String, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.DEBUG)) {
            log(LogEntry(level = LogLevel.DEBUG, message = message, context = context))
        }
    }

    /**
     * 记录 INFO 级别日志。
     *
     * @param message 日志消息
     * @param context 上下文信息
     */
    fun info(message: String, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.INFO)) {
            log(LogEntry(level = LogLevel.INFO, message = message, context = context))
        }
    }

    /**
     * 记录 WARN 级别日志。
     *
     * @param message 日志消息
     * @param context 上下文信息
     */
    fun warn(message: String, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.WARN)) {
            log(LogEntry(level = LogLevel.WARN, message = message, context = context))
        }
    }

    /**
     * 记录 WARN 级别日志，包含异常信息。
     *
     * @param message 日志消息
     * @param exception 异常
     * @param context 上下文信息
     */
    fun warn(message: String, exception: Throwable, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.WARN)) {
            log(LogEntry(level = LogLevel.WARN, message = message, exception = exception, context = context))
        }
    }

    /**
     * 记录 ERROR 级别日志。
     *
     * @param message 日志消息
     * @param context 上下文信息
     */
    fun error(message: String, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.ERROR)) {
            log(LogEntry(level = LogLevel.ERROR, message = message, context = context))
        }
    }

    /**
     * 记录 ERROR 级别日志，包含异常信息。
     *
     * @param message 日志消息
     * @param exception 异常
     * @param context 上下文信息
     */
    fun error(message: String, exception: Throwable, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.ERROR)) {
            log(LogEntry(level = LogLevel.ERROR, message = message, exception = exception, context = context))
        }
    }

    /**
     * 记录 FATAL 级别日志。
     *
     * @param message 日志消息
     * @param context 上下文信息
     */
    fun fatal(message: String, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.FATAL)) {
            log(LogEntry(level = LogLevel.FATAL, message = message, context = context))
        }
    }

    /**
     * 记录 FATAL 级别日志，包含异常信息。
     *
     * @param message 日志消息
     * @param exception 异常
     * @param context 上下文信息
     */
    fun fatal(message: String, exception: Throwable, context: Map<String, Any?> = emptyMap()) {
        if (isEnabled(LogLevel.FATAL)) {
            log(LogEntry(level = LogLevel.FATAL, message = message, exception = exception, context = context))
        }
    }
}
