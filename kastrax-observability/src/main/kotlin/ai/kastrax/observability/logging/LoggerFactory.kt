package ai.kastrax.observability.logging

/**
 * 日志工厂接口。
 */
interface LoggerFactory {
    /**
     * 获取指定名称的日志记录器。
     *
     * @param name 日志记录器名称
     * @return 日志记录器
     */
    fun getLogger(name: String): Logger

    /**
     * 获取指定类的日志记录器。
     *
     * @param clazz 类
     * @return 日志记录器
     */
    fun getLogger(clazz: Class<*>): Logger {
        return getLogger(clazz.name)
    }

    /**
     * 获取调用者类的日志记录器。
     *
     * @return 日志记录器
     */
    fun getLogger(): Logger {
        val caller = Thread.currentThread().stackTrace[2]
        return getLogger(caller.className)
    }
}

/**
 * 日志系统。
 */
object LoggingSystem {
    private var factory: LoggerFactory = ConsoleLoggerFactory()

    /**
     * 设置日志工厂。
     *
     * @param loggerFactory 日志工厂
     */
    fun setLoggerFactory(loggerFactory: LoggerFactory) {
        factory = loggerFactory
    }

    /**
     * 获取日志工厂。
     *
     * @return 日志工厂
     */
    fun getLoggerFactory(): LoggerFactory {
        return factory
    }

    /**
     * 获取指定名称的日志记录器。
     *
     * @param name 日志记录器名称
     * @return 日志记录器
     */
    fun getLogger(name: String): Logger {
        return factory.getLogger(name)
    }

    /**
     * 获取指定类的日志记录器。
     *
     * @param clazz 类
     * @return 日志记录器
     */
    fun getLogger(clazz: Class<*>): Logger {
        return factory.getLogger(clazz)
    }

    /**
     * 获取调用者类的日志记录器。
     *
     * @return 日志记录器
     */
    fun getLogger(): Logger {
        val caller = Thread.currentThread().stackTrace[2]
        return factory.getLogger(caller.className)
    }
}
