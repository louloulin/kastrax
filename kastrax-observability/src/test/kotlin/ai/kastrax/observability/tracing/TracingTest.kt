package ai.kastrax.observability.tracing

import ai.kastrax.observability.tracing.OTelSpanKind as SpanKind
import ai.kastrax.observability.tracing.OTelTextMapGetter
import ai.kastrax.observability.tracing.OTelTextMapSetter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TracingTest {
    @Test
    fun testNoopTracer() {
        // 创建空操作跟踪器
        val tracer = NoopTracer()

        // 创建跟踪范围
        val span = tracer.createSpan(
            name = "test_span",
            kind = SpanKind.CLIENT,
            attributes = mapOf("key" to "value")
        )

        // 验证跟踪范围
        assertEquals("test_span", span.getName(), "Span name should match")

        // 设置属性和事件
        span.setAttribute("string_key", "string_value")
            .setAttribute("long_key", 123L)
            .setAttribute("double_key", 45.67)
            .setAttribute("bool_key", true)
            .addEvent("test_event", mapOf("event_key" to "event_value"))
            .setSuccess()

        // 结束跟踪范围
        span.end()

        // 获取当前跟踪范围
        val currentSpan = tracer.getCurrentSpan()
        assertNull(currentSpan, "NoopTracer should not have a current span")

        // 获取当前上下文
        val context = tracer.getCurrentContext()
        assertNotNull(context, "NoopTracer should return a context")
    }

    @Test
    fun testTracingSystem() {
        // 设置跟踪器
        val tracer = NoopTracer()
        TracingSystem.setTracer(tracer)

        // 验证跟踪器
        assertEquals(tracer, TracingSystem.getTracer())

        // 使用 withSpan 执行代码块
        val result: String = TracingSystem.withSpan<String>(
            name = "test_operation",
            block = { span ->
                span.setAttribute("operation_key", "operation_value")
                "result"
            }
        )

        // 验证结果
        assertEquals("result", result)

        // 使用 withSpan 执行可能抛出异常的代码块
        try {
            TracingSystem.withSpan("failing_operation") { _ ->
                throw RuntimeException("Test exception")
            }
        } catch (e: RuntimeException) {
            assertEquals("Test exception", e.message)
        }
    }

    @Test
    fun testContextPropagation() {
        // 创建空操作跟踪器
        val tracer = NoopTracer()

        // 创建载体
        val carrier = mutableMapOf<String, String>()

        // 创建跟踪范围
        val span = tracer.createSpan("test_span")

        // 获取上下文
        val context = span.getContext()

        // 结束跟踪范围
        span.end()

        // 验证上下文
        assertNotNull(context, "Context should not be null")
    }
}
