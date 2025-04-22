package ai.kastrax.observability.profiling.annotation

/**
 * 标记需要进行性能分析的方法或类。
 *
 * @property name 性能分析会话名称，如果为空则使用方法名
 * @property profilerName 性能分析器名称，如果为空则使用默认分析器
 * @property tags 性能分析会话标签
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Profiled(
    val name: String = "",
    val profilerName: String = "",
    val tags: Array<String> = []
)
