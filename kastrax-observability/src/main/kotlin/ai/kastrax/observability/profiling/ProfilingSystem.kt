package ai.kastrax.observability.profiling

import ai.kastrax.observability.profiling.bottleneck.BottleneckDetectionConfig
import ai.kastrax.observability.profiling.bottleneck.BottleneckDetectionResult
import ai.kastrax.observability.profiling.bottleneck.BottleneckDetector
import ai.kastrax.observability.profiling.composite.CompositeProfiler
import ai.kastrax.observability.profiling.memory.MemoryProfiler
import ai.kastrax.observability.profiling.method.MethodInvocationProfiler
import ai.kastrax.observability.profiling.time.ExecutionTimeProfiler
import ai.kastrax.observability.logging.LoggingSystem
import java.util.concurrent.ConcurrentHashMap

/**
 * 性能分析系统。
 * 提供集中管理性能分析器的功能。
 */
object ProfilingSystem {
    private val logger = LoggingSystem.getLogger(ProfilingSystem::class.java)
    private val profilers = ConcurrentHashMap<String, Profiler>()
    private val bottleneckDetector = BottleneckDetector()
    private var defaultProfiler: Profiler

    init {
        // 创建并注册默认分析器
        val executionTimeProfiler = ExecutionTimeProfiler()
        val memoryProfiler = MemoryProfiler()
        val methodInvocationProfiler = MethodInvocationProfiler()
        
        registerProfiler(executionTimeProfiler)
        registerProfiler(memoryProfiler)
        registerProfiler(methodInvocationProfiler)
        
        // 创建并注册综合分析器
        val compositeProfiler = CompositeProfiler(
            profilers = listOf(executionTimeProfiler, memoryProfiler, methodInvocationProfiler)
        )
        registerProfiler(compositeProfiler)
        
        // 设置默认分析器为综合分析器
        defaultProfiler = compositeProfiler
    }

    /**
     * 注册性能分析器。
     *
     * @param profiler 要注册的性能分析器
     */
    fun registerProfiler(profiler: Profiler) {
        logger.debug("Registering profiler: ${profiler.getName()}")
        profilers[profiler.getName()] = profiler
    }

    /**
     * 取消注册性能分析器。
     *
     * @param name 要取消注册的性能分析器名称
     */
    fun unregisterProfiler(name: String) {
        logger.debug("Unregistering profiler: $name")
        profilers.remove(name)
    }

    /**
     * 获取所有注册的性能分析器。
     *
     * @return 所有注册的性能分析器的映射
     */
    fun getProfilers(): Map<String, Profiler> = profilers.toMap()

    /**
     * 获取指定名称的性能分析器。
     *
     * @param name 性能分析器名称
     * @return 性能分析器，如果找不到则返回null
     */
    fun getProfiler(name: String): Profiler? = profilers[name]

    /**
     * 获取指定类型的性能分析器。
     *
     * @param type 性能分析器类型
     * @return 指定类型的性能分析器列表
     */
    fun getProfilersByType(type: ProfilerType): List<Profiler> {
        return profilers.values.filter { it.getType() == type }
    }

    /**
     * 获取默认性能分析器。
     *
     * @return 默认性能分析器
     */
    fun getDefaultProfiler(): Profiler = defaultProfiler

    /**
     * 设置默认性能分析器。
     *
     * @param profiler 要设置为默认的性能分析器
     */
    fun setDefaultProfiler(profiler: Profiler) {
        defaultProfiler = profiler
    }

    /**
     * 开始一个性能分析会话。
     *
     * @param name 会话名称
     * @param profilerName 性能分析器名称，如果为null则使用默认分析器
     * @return 性能分析会话
     */
    fun startSession(name: String, profilerName: String? = null): ProfilingSession {
        val profiler = profilerName?.let { getProfiler(it) } ?: defaultProfiler
        return profiler.startSession(name)
    }

    /**
     * 开始一个性能分析会话，并执行指定的代码块。
     *
     * @param name 会话名称
     * @param profilerName 性能分析器名称，如果为null则使用默认分析器
     * @param block 要执行的代码块
     * @return 代码块的返回值
     */
    fun <T> withSession(
        name: String,
        profilerName: String? = null,
        block: (ProfilingSession) -> T
    ): T {
        val profiler = profilerName?.let { getProfiler(it) } ?: defaultProfiler
        return profiler.withSession(name, block)
    }

    /**
     * 检测性能瓶颈。
     *
     * @param result 性能分析结果
     * @param config 瓶颈检测配置
     * @return 瓶颈检测结果
     */
    fun detectBottlenecks(
        result: ProfilingResult,
        config: BottleneckDetectionConfig = BottleneckDetectionConfig()
    ): BottleneckDetectionResult {
        return bottleneckDetector.detectBottlenecks(result, config)
    }

    /**
     * 关闭性能分析系统。
     */
    fun shutdown() {
        logger.info("Shutting down profiling system")
        
        // 关闭所有分析器
        profilers.values.forEach { profiler ->
            if (profiler is CompositeProfiler) {
                profiler.shutdown()
            } else if (profiler is MemoryProfiler) {
                profiler.shutdown()
            }
        }
    }
}
