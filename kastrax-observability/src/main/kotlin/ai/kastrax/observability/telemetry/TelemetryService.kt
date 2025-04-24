package ai.kastrax.observability.telemetry

import ai.kastrax.observability.tracing.OTelSpanKind
import ai.kastrax.observability.tracing.Tracer
import ai.kastrax.observability.tracing.TraceSpan
import io.opentelemetry.api.trace.StatusCode
import java.util.concurrent.ConcurrentHashMap

/**
 * 遥测服务。
 *
 * 用于管理遥测系统，包括跟踪器、指标收集器等。
 *
 * @property tracer 跟踪器
 */
class TelemetryService(private val tracer: Tracer) {
    private val tracers = ConcurrentHashMap<String, Tracer>()
    private val aspects = ConcurrentHashMap<String, TelemetryAspect>()

    init {
        // 注册默认跟踪器
        tracers["default-tracer"] = tracer
        aspects["default-tracer"] = TelemetryAspect(tracer)
    }

    /**
     * 注册跟踪器。
     *
     * @param name 跟踪器名称
     * @param tracer 跟踪器
     */
    fun registerTracer(name: String, tracer: Tracer) {
        tracers[name] = tracer
        aspects[name] = TelemetryAspect(tracer)
    }

    /**
     * 获取跟踪器。
     *
     * @param name 跟踪器名称
     * @return 跟踪器
     */
    fun getTracer(name: String = "default-tracer"): Tracer {
        return tracers[name] ?: tracer
    }

    /**
     * 获取遥测切面。
     *
     * @param name 跟踪器名称
     * @return 遥测切面
     */
    fun getAspect(name: String = "default-tracer"): TelemetryAspect {
        return aspects[name] ?: TelemetryAspect(tracer)
    }

    /**
     * 创建跟踪范围。
     *
     * @param name 范围名称
     * @param kind 范围类型
     * @param attributes 属性
     * @param tracerName 跟踪器名称
     * @return 跟踪范围
     */
    fun createSpan(
        name: String,
        kind: OTelSpanKind = OTelSpanKind.INTERNAL,
        attributes: Map<String, Any> = emptyMap(),
        tracerName: String = "default-tracer"
    ): TraceSpan {
        val tracer = getTracer(tracerName)
        return tracer.createSpan(name, kind, attributes)
    }

    /**
     * 执行带有遥测跟踪的方法。
     *
     * @param spanName 跟踪范围名称
     * @param spanKind 跟踪范围类型
     * @param skipIfNoTelemetry 如果没有遥测系统，是否跳过跟踪
     * @param tracerName 跟踪器名称
     * @param args 方法参数
     * @param block 方法体
     * @return 方法返回值
     */
    fun <T> executeWithSpan(
        spanName: String,
        spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
        skipIfNoTelemetry: Boolean = true,
        tracerName: String = "default-tracer",
        args: Array<out Any?> = emptyArray(),
        block: () -> T
    ): T {
        val tracer = getTracer(tracerName)
        return TelemetryUtils.executeWithSpan(
            tracer = tracer,
            spanName = spanName,
            spanKind = spanKind,
            skipIfNoTelemetry = skipIfNoTelemetry,
            tracerName = tracerName,
            args = args,
            block = block
        )
    }

    /**
     * 拦截带有 WithSpan 注解的方法，添加遥测跟踪。
     *
     * @param joinPoint 连接点
     * @param target 目标对象
     * @param methodName 方法名
     * @param args 方法参数
     * @param tracerName 跟踪器名称
     * @return 方法返回值
     */
    fun <T> aroundWithSpan(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>,
        tracerName: String = "default-tracer"
    ): T {
        val aspect = getAspect(tracerName)
        return aspect.aroundWithSpan(joinPoint, target, methodName, args)
    }

    /**
     * 拦截带有 InstrumentClass 注解的类，为类中的所有方法添加遥测跟踪。
     *
     * @param joinPoint 连接点
     * @param target 目标对象
     * @param methodName 方法名
     * @param args 方法参数
     * @param tracerName 跟踪器名称
     * @return 方法返回值
     */
    fun <T> aroundInstrumentClass(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>,
        tracerName: String = "default-tracer"
    ): T {
        val aspect = getAspect(tracerName)
        return aspect.aroundInstrumentClass(joinPoint, target, methodName, args)
    }

    companion object {
        /**
         * 创建默认的遥测服务。
         *
         * @param tracer 跟踪器
         * @return 遥测服务
         */
        fun createDefault(tracer: Tracer): TelemetryService {
            return TelemetryService(tracer)
        }
    }
}
