package ai.kastrax.observability.logging

/**
 * 日志上下文，提供 MDC（Mapped Diagnostic Context）支持。
 */
object LogContext {
    private val threadLocal = ThreadLocal.withInitial { mutableMapOf<String, Any?>() }

    /**
     * 获取当前线程的上下文映射。
     *
     * @return 上下文映射
     */
    fun getContext(): Map<String, Any?> {
        return threadLocal.get().toMap()
    }

    /**
     * 设置上下文值。
     *
     * @param key 键
     * @param value 值
     */
    fun put(key: String, value: Any?) {
        threadLocal.get()[key] = value
    }

    /**
     * 获取上下文值。
     *
     * @param key 键
     * @return 值，如果不存在则返回 null
     */
    fun get(key: String): Any? {
        return threadLocal.get()[key]
    }

    /**
     * 移除上下文值。
     *
     * @param key 键
     * @return 被移除的值，如果不存在则返回 null
     */
    fun remove(key: String): Any? {
        return threadLocal.get().remove(key)
    }

    /**
     * 清除所有上下文值。
     */
    fun clear() {
        threadLocal.get().clear()
    }

    /**
     * 在指定上下文中执行代码块。
     *
     * @param context 上下文映射
     * @param block 代码块
     * @return 代码块的返回值
     */
    fun <T> withContext(context: Map<String, Any?>, block: () -> T): T {
        val oldContext = getContext()
        try {
            context.forEach { (key, value) -> put(key, value) }
            return block()
        } finally {
            clear()
            oldContext.forEach { (key, value) -> put(key, value) }
        }
    }

    /**
     * 在指定上下文中执行代码块。
     *
     * @param key 键
     * @param value 值
     * @param block 代码块
     * @return 代码块的返回值
     */
    fun <T> withContext(key: String, value: Any?, block: () -> T): T {
        return withContext(mapOf(key to value), block)
    }
}
