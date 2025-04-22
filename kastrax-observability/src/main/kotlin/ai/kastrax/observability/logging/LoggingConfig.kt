package ai.kastrax.observability.logging

/**
 * 日志配置类。
 */
class LoggingConfig {
    var level: LogLevel = LogLevel.INFO
    var console: Boolean = true
    var file: FileConfig? = null
    var json: JsonConfig? = null

    /**
     * 文件日志配置。
     */
    class FileConfig {
        var enabled: Boolean = false
        var directory: String = "logs"
        var filePrefix: String = "app"
        var maxFileSize: Long = 10 * 1024 * 1024 // 10MB
        var maxFiles: Int = 10
    }

    /**
     * JSON 日志配置。
     */
    class JsonConfig {
        var enabled: Boolean = false
        var directory: String = "logs"
        var filePrefix: String = "app"
        var maxFileSize: Long = 10 * 1024 * 1024 // 10MB
        var maxFiles: Int = 10
    }

    /**
     * 应用日志配置。
     */
    fun apply() {
        // 创建日志工厂
        val factories = mutableListOf<LoggerFactory>()

        // 控制台日志
        if (console) {
            val consoleFactory = ConsoleLoggerFactory()
            consoleFactory.setMinLevel(level)
            factories.add(consoleFactory)
        }

        // 文件日志
        file?.let {
            if (it.enabled) {
                val fileFactory = FileLoggerFactory(
                    directory = it.directory,
                    filePrefix = it.filePrefix,
                    maxFileSize = it.maxFileSize,
                    maxFiles = it.maxFiles
                )
                fileFactory.setMinLevel(level)
                factories.add(fileFactory)
            }
        }

        // JSON 日志
        json?.let {
            if (it.enabled) {
                val jsonFactory = JsonLoggerFactory(
                    directory = it.directory,
                    filePrefix = it.filePrefix,
                    maxFileSize = it.maxFileSize,
                    maxFiles = it.maxFiles
                )
                jsonFactory.setMinLevel(level)
                factories.add(jsonFactory)
            }
        }

        // 设置日志工厂
        if (factories.isNotEmpty()) {
            if (factories.size == 1) {
                LoggingSystem.setLoggerFactory(factories.first())
            } else {
                LoggingSystem.setLoggerFactory(CompositeLoggerFactory(factories))
            }
        }
    }
}

/**
 * 组合日志记录器工厂。
 *
 * @property factories 日志记录器工厂列表
 */
class CompositeLoggerFactory(
    private val factories: List<LoggerFactory>
) : LoggerFactory {
    override fun getLogger(name: String): Logger {
        val loggers = factories.map { it.getLogger(name) }
        return CompositeLogger(name, loggers)
    }
}

/**
 * 组合日志记录器。
 *
 * @property name 日志记录器名称
 * @property loggers 日志记录器列表
 */
class CompositeLogger(
    private val name: String,
    private val loggers: List<Logger>
) : Logger {
    override fun getName(): String {
        return name
    }

    override fun isEnabled(level: LogLevel): Boolean {
        return loggers.any { it.isEnabled(level) }
    }

    override fun log(entry: LogEntry) {
        loggers.forEach { logger ->
            if (logger.isEnabled(entry.level)) {
                logger.log(entry)
            }
        }
    }
}
