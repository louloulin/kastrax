package ai.kastrax.observability.tracing

import ai.kastrax.observability.tracing.OTelSpanKind
import ai.kastrax.observability.tracing.OTelScope

/**
 * 跟踪系统。
 */
object TracingSystem {
    private var tracer: Tracer = NoopTracer()

    /**
     * 设置跟踪器。
     *
     * @param tracer 跟踪器
     */
    fun setTracer(tracer: Tracer) {
        this.tracer = tracer
    }

    /**
     * 获取跟踪器。
     *
     * @return 跟踪器
     */
    fun getTracer(): Tracer {
        return tracer
    }

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
    ): TraceSpan {
        return tracer.createSpan(name, kind, attributes)
    }

    /**
     * 获取当前活动的跟踪范围。
     *
     * @return 当前活动的跟踪范围，如果不存在则返回 null
     */
    fun getCurrentSpan(): TraceSpan? {
        return tracer.getCurrentSpan()
    }

    /**
     * 获取当前跟踪上下文。
     *
     * @return 当前跟踪上下文
     */
    fun getCurrentContext(): TraceContext {
        return tracer.getCurrentContext()
    }

    /**
     * 在跟踪范围内执行代码块。
     *
     * @param name 范围名称
     * @param kind 范围类型
     * @param attributes 属性
     * @param block 代码块
     * @return 代码块的返回值
     */
    fun <T> withSpan(
        name: String,
        kind: OTelSpanKind = OTelSpanKind.INTERNAL,
        attributes: Map<String, Any> = emptyMap(),
        block: (TraceSpan) -> T
    ): T {
        val span = createSpan(name, kind, attributes)
        var scope: OTelScope? = null
        try {
            scope = span.makeCurrent()
            return block(span)
        } catch (e: Exception) {
            span.setError("Exception: ${e.message}")
            span.recordException(e)
            throw e
        } finally {
            scope?.close()
            span.end()
        }
    }

    /**
     * 在跟踪范围内执行代码块。
     *
     * @param name 范围名称
     * @param kind 范围类型
     * @param attributes 属性
     * @param block 代码块
     */
    fun withSpan(
        name: String,
        kind: OTelSpanKind = OTelSpanKind.INTERNAL,
        attributes: Map<String, Any> = emptyMap(),
        block: (TraceSpan) -> Unit
    ) {
        val span = createSpan(name, kind, attributes)
        var scope: OTelScope? = null
        try {
            scope = span.makeCurrent()
            block(span)
        } catch (e: Exception) {
            span.setError("Exception: ${e.message}")
            span.recordException(e)
            throw e
        } finally {
            scope?.close()
            span.end()
        }
    }
}
