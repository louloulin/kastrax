package ai.kastrax.observability.profiling

/**
 * 性能分析器接口。
 * 定义了性能分析的基本操作。
 */
interface Profiler {
    /**
     * 开始一个性能分析会话。
     *
     * @param name 会话名称
     * @return 性能分析会话
     */
    fun startSession(name: String): ProfilingSession

    /**
     * 开始一个性能分析会话，并执行指定的代码块。
     *
     * @param name 会话名称
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    fun <T> withSession(name: String, block: (ProfilingSession) -> T): T

    /**
     * 获取性能分析器的名称。
     *
     * @return 性能分析器名称
     */
    fun getName(): String

    /**
     * 获取性能分析器的类型。
     *
     * @return 性能分析器类型
     */
    fun getType(): ProfilerType

    /**
     * 获取所有活动的性能分析会话。
     *
     * @return 活动的性能分析会话列表
     */
    fun getActiveSessions(): List<ProfilingSession>

    /**
     * 获取所有已完成的性能分析会话。
     *
     * @return 已完成的性能分析会话列表
     */
    fun getCompletedSessions(): List<ProfilingSession>

    /**
     * 清除所有已完成的性能分析会话。
     */
    fun clearCompletedSessions()
}

/**
 * 性能分析器类型枚举。
 */
enum class ProfilerType {
    /**
     * 执行时间分析器。
     */
    EXECUTION_TIME,

    /**
     * 内存使用分析器。
     */
    MEMORY_USAGE,

    /**
     * CPU使用分析器。
     */
    CPU_USAGE,

    /**
     * 方法调用分析器。
     */
    METHOD_INVOCATION,

    /**
     * 综合性能分析器。
     */
    COMPOSITE
}
