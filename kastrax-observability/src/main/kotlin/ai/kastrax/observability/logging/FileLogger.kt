package ai.kastrax.observability.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 文件日志记录器工厂。
 *
 * @property directory 日志文件目录
 * @property filePrefix 日志文件前缀
 * @property maxFileSize 单个日志文件的最大大小（字节）
 * @property maxFiles 最大日志文件数量
 */
class FileLoggerFactory(
    private val directory: String,
    private val filePrefix: String = "app",
    private val maxFileSize: Long = 10 * 1024 * 1024, // 10MB
    private val maxFiles: Int = 10
) : LoggerFactory {
    private val loggers = ConcurrentHashMap<String, Logger>()
    private var minLevel = LogLevel.INFO
    private val logQueue = LinkedBlockingQueue<LogEntry>()
    private val executor = Executors.newSingleThreadExecutor { r ->
        val thread = Thread(r, "FileLogger-Worker")
        thread.isDaemon = true
        thread
    }
    private var currentFile: File? = null
    private var currentWriter: PrintWriter? = null
    private var currentFileSize: Long = 0

    init {
        // 创建日志目录
        val dir = File(directory)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // 启动日志处理线程
        executor.submit {
            try {
                processLogs()
            } catch (e: Exception) {
                System.err.println("Error in log processing thread: ${e.message}")
                e.printStackTrace()
            }
        }

        // 添加 JVM 关闭钩子
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown()
        })
    }

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
        return loggers.computeIfAbsent(name) { FileLogger(name, this) }
    }

    /**
     * 添加日志条目到队列。
     *
     * @param entry 日志条目
     */
    internal fun addLogEntry(entry: LogEntry) {
        logQueue.offer(entry)
    }

    /**
     * 处理日志队列中的日志条目。
     */
    private fun processLogs() {
        while (!Thread.currentThread().isInterrupted) {
            try {
                val entry = logQueue.poll(100, TimeUnit.MILLISECONDS)
                if (entry != null) {
                    writeLogEntry(entry)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                System.err.println("Error processing log entry: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 写入日志条目到文件。
     *
     * @param entry 日志条目
     */
    private fun writeLogEntry(entry: LogEntry) {
        ensureWriterOpen()

        val writer = currentWriter ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val levelStr = entry.level.name.padEnd(5)
        val contextStr = if (entry.context.isNotEmpty()) {
            " " + entry.context.entries.joinToString(", ") { "${it.key}=${formatValue(it.value)}" }
        } else {
            ""
        }

        val logLine = "$timestamp [$levelStr] ${entry.message}$contextStr"
        writer.println(logLine)

        if (entry.exception != null) {
            entry.exception.printStackTrace(writer)
        }

        writer.flush()
        currentFileSize += logLine.length + 1 // +1 for newline
        
        // 检查文件大小是否超过限制
        if (currentFileSize >= maxFileSize) {
            rotateLogFile()
        }
    }

    /**
     * 确保日志写入器已打开。
     */
    private fun ensureWriterOpen() {
        if (currentWriter == null) {
            rotateLogFile()
        }
    }

    /**
     * 轮转日志文件。
     */
    private fun rotateLogFile() {
        // 关闭当前写入器
        currentWriter?.close()
        currentWriter = null
        
        // 清理旧日志文件
        cleanupOldLogFiles()
        
        // 创建新日志文件
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val newFile = File(directory, "${filePrefix}-${timestamp}.log")
        
        try {
            newFile.createNewFile()
            currentWriter = PrintWriter(FileWriter(newFile, true))
            currentFile = newFile
            currentFileSize = newFile.length()
        } catch (e: Exception) {
            System.err.println("Failed to create new log file: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 清理旧日志文件。
     */
    private fun cleanupOldLogFiles() {
        val dir = File(directory)
        val logFiles = dir.listFiles { file ->
            file.isFile && file.name.startsWith(filePrefix) && file.name.endsWith(".log")
        }

        if (logFiles != null && logFiles.size >= maxFiles) {
            // 按修改时间排序
            val sortedFiles = logFiles.sortedBy { it.lastModified() }
            
            // 删除最旧的文件，直到文件数量低于限制
            for (i in 0 until sortedFiles.size - maxFiles + 1) {
                try {
                    sortedFiles[i].delete()
                } catch (e: Exception) {
                    System.err.println("Failed to delete old log file ${sortedFiles[i].name}: ${e.message}")
                }
            }
        }
    }

    /**
     * 关闭日志系统。
     */
    fun shutdown() {
        executor.shutdown()
        try {
            // 等待所有日志写入完成
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }

        // 关闭写入器
        currentWriter?.close()
        currentWriter = null
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

/**
 * 文件日志记录器。
 *
 * @property name 日志记录器名称
 * @property factory 日志记录器工厂
 */
class FileLogger(
    private val name: String,
    private val factory: FileLoggerFactory
) : Logger {
    override fun getName(): String {
        return name
    }

    override fun isEnabled(level: LogLevel): Boolean {
        return level.ordinal >= factory.getMinLevel().ordinal
    }

    override fun log(entry: LogEntry) {
        // 添加 MDC 上下文
        val contextWithMdc = entry.context.toMutableMap()
        LogContext.getContext().forEach { (key, value) ->
            if (key !in contextWithMdc) {
                contextWithMdc[key] = value
            }
        }

        // 添加日志记录器名称
        val entryWithName = entry.copy(
            context = contextWithMdc + ("logger" to name)
        )

        factory.addLogEntry(entryWithName)
    }
}
