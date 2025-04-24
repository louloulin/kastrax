package ai.kastrax.observability.telemetry

import ai.kastrax.observability.tracing.OTelSpanKind
import ai.kastrax.observability.tracing.Tracer
import io.opentelemetry.api.trace.StatusCode
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

/**
 * 遥测跟踪注解。
 *
 * 用于标记需要进行遥测跟踪的方法。
 *
 * @property spanName 跟踪范围名称，默认为方法名
 * @property spanKind 跟踪范围类型，默认为 INTERNAL
 * @property skipIfNoTelemetry 如果没有遥测系统，是否跳过跟踪，默认为 true
 * @property tracerName 跟踪器名称，默认为 "default-tracer"
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithSpan(
    val spanName: String = "",
    val spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
    val skipIfNoTelemetry: Boolean = true,
    val tracerName: String = "default-tracer"
)

/**
 * 遥测类注解。
 *
 * 用于标记需要进行遥测跟踪的类，会自动为类中的所有方法添加遥测跟踪。
 *
 * @property prefix 跟踪范围名称前缀，默认为类名
 * @property spanKind 跟踪范围类型，默认为 INTERNAL
 * @property excludeMethods 排除的方法名列表
 * @property tracerName 跟踪器名称，默认为 "default-tracer"
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InstrumentClass(
    val prefix: String = "",
    val spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
    val excludeMethods: Array<String> = [],
    val tracerName: String = "default-tracer"
)

/**
 * 遥测工具类。
 */
object TelemetryUtils {
    /**
     * 检查是否有活动的遥测系统。
     *
     * @param tracerName 跟踪器名称
     * @return 是否有活动的遥测系统
     */
    fun hasActiveTelemetry(tracerName: String = "default-tracer"): Boolean {
        // 这里简化实现，实际应该检查遥测系统是否可用
        return true
    }

    /**
     * 执行带有遥测跟踪的方法。
     *
     * @param tracer 跟踪器
     * @param spanName 跟踪范围名称
     * @param spanKind 跟踪范围类型
     * @param skipIfNoTelemetry 如果没有遥测系统，是否跳过跟踪
     * @param tracerName 跟踪器名称
     * @param args 方法参数
     * @param block 方法体
     * @return 方法返回值
     */
    fun <T> executeWithSpan(
        tracer: Tracer,
        spanName: String,
        spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
        skipIfNoTelemetry: Boolean = true,
        tracerName: String = "default-tracer",
        args: Array<out Any?> = emptyArray(),
        block: () -> T
    ): T {
        // 如果没有遥测系统且设置了跳过，则直接执行方法
        if (skipIfNoTelemetry && !hasActiveTelemetry(tracerName)) {
            return block()
        }

        // 创建跟踪范围
        val span = tracer.createSpan(spanName, spanKind)

        // 记录输入参数
        args.forEachIndexed { index, arg ->
            try {
                span.setAttribute("$spanName.argument.$index", arg.toString())
            } catch (e: Exception) {
                span.setAttribute("$spanName.argument.$index", "[Not Serializable]")
            }
        }

        // 执行方法
        return try {
            val result = block()

            // 记录返回值
            try {
                span.setAttribute("$spanName.result", result.toString())
            } catch (e: Exception) {
                span.setAttribute("$spanName.result", "[Not Serializable]")
            }

            result
        } catch (e: Exception) {
            // 记录异常
            span.setError(e.message ?: "Unknown error")
            span.recordException(e)
            throw e
        } finally {
            // 结束跟踪范围
            span.end()
        }
    }

    /**
     * 为类中的所有方法添加遥测跟踪。
     *
     * @param klass 类
     * @param tracer 跟踪器
     * @param prefix 跟踪范围名称前缀
     * @param spanKind 跟踪范围类型
     * @param excludeMethods 排除的方法名列表
     * @param tracerName 跟踪器名称
     */
    fun instrumentClass(
        klass: KClass<*>,
        tracer: Tracer,
        prefix: String = klass.simpleName ?: "",
        spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
        excludeMethods: List<String> = emptyList(),
        tracerName: String = "default-tracer"
    ) {
        // 获取类中的所有方法
        val methods = klass.functions

        // 为每个方法添加遥测跟踪
        methods.forEach { method ->
            // 跳过排除的方法
            if (method.name in excludeMethods || method.name == "constructor") {
                return@forEach
            }

            // 如果方法已经有 WithSpan 注解，则跳过
            if (method.hasAnnotation<WithSpan>()) {
                return@forEach
            }

            // 为方法添加遥测跟踪
            val spanName = if (prefix.isNotEmpty()) "$prefix.${method.name}" else method.name
            // 注意：这里只是示例，实际上无法在运行时为方法添加注解
            // 这里应该使用 AOP 或其他技术来实现
        }
    }
}

/**
 * 遥测切面。
 *
 * 用于拦截带有 WithSpan 注解的方法，添加遥测跟踪。
 */
class TelemetryAspect(private val tracer: Tracer) {
    /**
     * 拦截带有 WithSpan 注解的方法，添加遥测跟踪。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     */
    fun <T> aroundWithSpan(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>
    ): T {
        // 获取方法上的 WithSpan 注解
        val method = target::class.functions.find { it.name == methodName }
        val annotation = method?.findAnnotation<WithSpan>()

        // 如果没有注解，则直接执行方法
        if (annotation == null) {
            return joinPoint()
        }

        // 获取注解参数
        val spanName = if (annotation.spanName.isNotEmpty()) annotation.spanName else methodName
        val spanKind = annotation.spanKind
        val skipIfNoTelemetry = annotation.skipIfNoTelemetry
        val tracerName = annotation.tracerName

        // 执行带有遥测跟踪的方法
        return TelemetryUtils.executeWithSpan(
            tracer = tracer,
            spanName = spanName,
            spanKind = spanKind,
            skipIfNoTelemetry = skipIfNoTelemetry,
            tracerName = tracerName,
            args = args,
            block = joinPoint
        )
    }

    /**
     * 拦截带有 InstrumentClass 注解的类，为类中的所有方法添加遥测跟踪。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     */
    fun <T> aroundInstrumentClass(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>
    ): T {
        // 获取类上的 InstrumentClass 注解
        val annotation = target::class.findAnnotation<InstrumentClass>()

        // 如果没有注解，则直接执行方法
        if (annotation == null) {
            return joinPoint()
        }

        // 获取注解参数
        val prefix = if (annotation.prefix.isNotEmpty()) annotation.prefix else target::class.simpleName ?: ""
        val spanKind = annotation.spanKind
        val excludeMethods = annotation.excludeMethods.toList()
        val tracerName = annotation.tracerName

        // 如果方法在排除列表中，则直接执行方法
        if (methodName in excludeMethods || methodName == "constructor") {
            return joinPoint()
        }

        // 执行带有遥测跟踪的方法
        val spanName = if (prefix.isNotEmpty()) "$prefix.$methodName" else methodName
        return TelemetryUtils.executeWithSpan(
            tracer = tracer,
            spanName = spanName,
            spanKind = spanKind,
            skipIfNoTelemetry = true,
            tracerName = tracerName,
            args = args,
            block = joinPoint
        )
    }
}
