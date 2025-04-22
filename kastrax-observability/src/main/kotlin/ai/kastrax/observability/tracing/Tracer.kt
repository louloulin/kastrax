package ai.kastrax.observability.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter
import java.util.concurrent.TimeUnit

// 重新导出 OpenTelemetry 类型，以便在其他文件中使用
typealias OTelSpanKind = SpanKind
typealias OTelTextMapGetter<C> = TextMapGetter<C>
typealias OTelTextMapSetter<C> = TextMapSetter<C>
typealias OTelScope = Scope

/**
 * 跟踪器接口。
 */
interface Tracer {
    /**
     * 创建跟踪范围。
     *
     * @param name 范围名称
     * @param kind 范围类型
     * @param attributes 属性
     * @return 跟踪范围
     */
    fun createSpan(
        name: String,
        kind: OTelSpanKind = OTelSpanKind.INTERNAL,
        attributes: Map<String, Any> = emptyMap()
    ): TraceSpan

    /**
     * 获取当前活动的跟踪范围。
     *
     * @return 当前活动的跟踪范围，如果不存在则返回 null
     */
    fun getCurrentSpan(): TraceSpan?

    /**
     * 获取当前跟踪上下文。
     *
     * @return 当前跟踪上下文
     */
    fun getCurrentContext(): TraceContext

    /**
     * 从载体中提取跟踪上下文。
     *
     * @param carrier 载体
     * @param getter 文本映射获取器
     * @return 跟踪上下文
     */
    fun <C> extractContext(carrier: C, getter: OTelTextMapGetter<C>): TraceContext

    /**
     * 将跟踪上下文注入到载体中。
     *
     * @param context 跟踪上下文
     * @param carrier 载体
     * @param setter 文本映射设置器
     */
    fun <C> injectContext(context: TraceContext, carrier: C, setter: OTelTextMapSetter<C>)
}

/**
 * 跟踪范围接口。
 */
interface TraceSpan : AutoCloseable {
    /**
     * 获取跟踪范围名称。
     *
     * @return 跟踪范围名称
     */
    fun getName(): String

    /**
     * 设置属性。
     *
     * @param key 键
     * @param value 值
     * @return 跟踪范围
     */
    fun setAttribute(key: String, value: String): TraceSpan

    /**
     * 设置属性。
     *
     * @param key 键
     * @param value 值
     * @return 跟踪范围
     */
    fun setAttribute(key: String, value: Long): TraceSpan

    /**
     * 设置属性。
     *
     * @param key 键
     * @param value 值
     * @return 跟踪范围
     */
    fun setAttribute(key: String, value: Double): TraceSpan

    /**
     * 设置属性。
     *
     * @param key 键
     * @param value 值
     * @return 跟踪范围
     */
    fun setAttribute(key: String, value: Boolean): TraceSpan

    /**
     * 添加事件。
     *
     * @param name 事件名称
     * @param attributes 属性
     * @return 跟踪范围
     */
    fun addEvent(name: String, attributes: Map<String, Any> = emptyMap()): TraceSpan

    /**
     * 设置状态为成功。
     *
     * @return 跟踪范围
     */
    fun setSuccess(): TraceSpan

    /**
     * 设置状态为错误。
     *
     * @param description 错误描述
     * @return 跟踪范围
     */
    fun setError(description: String): TraceSpan

    /**
     * 记录异常。
     *
     * @param exception 异常
     * @return 跟踪范围
     */
    fun recordException(exception: Throwable): TraceSpan

    /**
     * 结束跟踪范围。
     */
    fun end()

    /**
     * 获取跟踪上下文。
     *
     * @return 跟踪上下文
     */
    fun getContext(): TraceContext

    /**
     * 使跟踪范围成为当前活动的范围。
     *
     * @return 作用域，用于恢复先前的活动范围
     */
    fun makeCurrent(): OTelScope
}

/**
 * 跟踪上下文接口。
 */
interface TraceContext {
    /**
     * 获取底层上下文。
     *
     * @return 底层上下文
     */
    fun getUnderlyingContext(): Any
}

/**
 * OpenTelemetry 跟踪器实现。
 *
 * @property openTelemetry OpenTelemetry 实例
 * @property instrumentationName 仪表名称
 * @property instrumentationVersion 仪表版本
 */
class OpenTelemetryTracer(
    private val openTelemetry: OpenTelemetry,
    private val instrumentationName: String,
    private val instrumentationVersion: String? = null
) : Tracer {
    private val tracer = openTelemetry.tracerProvider.tracerBuilder(instrumentationName)
        .setInstrumentationVersion(instrumentationVersion)
        .build()

    override fun createSpan(
        name: String,
        kind: OTelSpanKind,
        attributes: Map<String, Any>
    ): TraceSpan {
        val spanBuilder = tracer.spanBuilder(name)
            .setSpanKind(kind)

        // 设置属性
        val attributesBuilder = Attributes.builder()
        for ((key, value) in attributes) {
            when (value) {
                is String -> attributesBuilder.put(key, value)
                is Long -> attributesBuilder.put(key, value)
                is Double -> attributesBuilder.put(key, value)
                is Boolean -> attributesBuilder.put(key, value)
                else -> attributesBuilder.put(key, value.toString())
            }
        }
        spanBuilder.setAllAttributes(attributesBuilder.build())

        // 创建跟踪范围
        val span = spanBuilder.startSpan()
        val scope = span.makeCurrent()

        return OpenTelemetryTraceSpan(span, scope, name)
    }

    override fun getCurrentSpan(): TraceSpan? {
        val span = Span.current()
        if (span == Span.getInvalid()) {
            return null
        }
        return OpenTelemetryTraceSpan(span, null, "current")
    }

    override fun getCurrentContext(): TraceContext {
        return OpenTelemetryTraceContext(Context.current())
    }

    override fun <C> extractContext(carrier: C, getter: OTelTextMapGetter<C>): TraceContext {
        val context = openTelemetry.propagators.textMapPropagator
            .extract(Context.current(), carrier, getter)
        return OpenTelemetryTraceContext(context)
    }

    override fun <C> injectContext(context: TraceContext, carrier: C, setter: OTelTextMapSetter<C>) {
        val otelContext = context.getUnderlyingContext() as Context
        openTelemetry.propagators.textMapPropagator
            .inject(otelContext, carrier, setter)
    }
}

/**
 * OpenTelemetry 跟踪范围实现。
 *
 * @property span OpenTelemetry 跟踪范围
 * @property scope 作用域
 * @property name 范围名称
 */
class OpenTelemetryTraceSpan(
    private val span: Span,
    private val scope: Scope?,
    private val name: String
) : TraceSpan {
    private var closed = false

    override fun getName(): String {
        return name
    }

    override fun setAttribute(key: String, value: String): TraceSpan {
        span.setAttribute(key, value)
        return this
    }

    override fun setAttribute(key: String, value: Long): TraceSpan {
        span.setAttribute(key, value)
        return this
    }

    override fun setAttribute(key: String, value: Double): TraceSpan {
        span.setAttribute(key, value)
        return this
    }

    override fun setAttribute(key: String, value: Boolean): TraceSpan {
        span.setAttribute(key, value)
        return this
    }

    override fun addEvent(name: String, attributes: Map<String, Any>): TraceSpan {
        val attributesBuilder = Attributes.builder()
        for ((key, value) in attributes) {
            when (value) {
                is String -> attributesBuilder.put(key, value)
                is Long -> attributesBuilder.put(key, value)
                is Double -> attributesBuilder.put(key, value)
                is Boolean -> attributesBuilder.put(key, value)
                else -> attributesBuilder.put(key, value.toString())
            }
        }
        span.addEvent(name, attributesBuilder.build())
        return this
    }

    override fun setSuccess(): TraceSpan {
        span.setStatus(StatusCode.OK)
        return this
    }

    override fun setError(description: String): TraceSpan {
        span.setStatus(StatusCode.ERROR, description)
        return this
    }

    override fun recordException(exception: Throwable): TraceSpan {
        span.recordException(exception)
        return this
    }

    override fun end() {
        if (!closed) {
            span.end()
            scope?.close()
            closed = true
        }
    }

    override fun getContext(): TraceContext {
        return OpenTelemetryTraceContext(Context.current())
    }

    override fun makeCurrent(): OTelScope {
        return span.makeCurrent()
    }

    override fun close() {
        end()
    }
}

/**
 * OpenTelemetry 跟踪上下文实现。
 *
 * @property context OpenTelemetry 上下文
 */
class OpenTelemetryTraceContext(
    private val context: Context
) : TraceContext {
    override fun getUnderlyingContext(): Any {
        return context
    }
}
