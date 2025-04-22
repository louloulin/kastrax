package ai.kastrax.observability.tracing

import ai.kastrax.observability.tracing.OTelSpanKind
import ai.kastrax.observability.tracing.OTelTextMapGetter
import ai.kastrax.observability.tracing.OTelTextMapSetter
import ai.kastrax.observability.tracing.OTelScope

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

/**
 * 跟踪配置类。
 */
class TracingConfig {
    var enabled: Boolean = true
    var serviceName: String = "kastrax-service"
    var serviceVersion: String = "1.0.0"
    var serviceNamespace: String = "kastrax"
    var serviceInstanceId: String = "instance-1"
    var samplingRate: Double = 1.0
    var exporters: ExportersConfig = ExportersConfig()
    var propagation: PropagationConfig = PropagationConfig()

    /**
     * 导出器配置。
     */
    class ExportersConfig {
        var logging: LoggingExporterConfig = LoggingExporterConfig()
        var otlp: OtlpExporterConfig = OtlpExporterConfig()
    }

    /**
     * 日志导出器配置。
     */
    class LoggingExporterConfig {
        var enabled: Boolean = false
    }

    /**
     * OTLP 导出器配置。
     */
    class OtlpExporterConfig {
        var enabled: Boolean = false
        var endpoint: String = "http://localhost:4318/v1/traces"
        var headers: Map<String, String> = emptyMap()
        var timeout: Long = 10000
    }

    /**
     * 传播配置。
     */
    class PropagationConfig {
        var w3c: Boolean = true
        var b3: Boolean = false
        var jaeger: Boolean = false
    }

    /**
     * 应用跟踪配置。
     *
     * @return 跟踪器
     */
    fun apply(): Tracer {
        if (!enabled) {
            return NoopTracer()
        }

        // 创建资源
        val resource = Resource.getDefault()
            .merge(
                Resource.create(
                    Attributes.builder()
                        .put(ResourceAttributes.SERVICE_NAME, serviceName)
                        .put(ResourceAttributes.SERVICE_VERSION, serviceVersion)
                        .put(ResourceAttributes.SERVICE_NAMESPACE, serviceNamespace)
                        .put(ResourceAttributes.SERVICE_INSTANCE_ID, serviceInstanceId)
                        .build()
                )
            )

        // 创建采样器
        val sampler = if (samplingRate >= 1.0) {
            Sampler.alwaysOn()
        } else if (samplingRate <= 0.0) {
            Sampler.alwaysOff()
        } else {
            Sampler.traceIdRatioBased(samplingRate)
        }

        // 创建跟踪提供者构建器
        val tracerProviderBuilder = SdkTracerProvider.builder()
            .setResource(resource)
            .setSampler(sampler)

        // 添加日志导出器
        if (exporters.logging.enabled) {
            tracerProviderBuilder.addSpanProcessor(
                SimpleSpanProcessor.create(LoggingSpanExporter.create())
            )
        }

        // 添加 OTLP 导出器
        if (exporters.otlp.enabled) {
            val otlpExporterBuilder = OtlpHttpSpanExporter.builder()
                .setEndpoint(exporters.otlp.endpoint)
                .setTimeout(exporters.otlp.timeout, java.util.concurrent.TimeUnit.MILLISECONDS)

            // 添加头部
            exporters.otlp.headers.forEach { (key, value) ->
                otlpExporterBuilder.addHeader(key, value)
            }

            tracerProviderBuilder.addSpanProcessor(
                BatchSpanProcessor.builder(otlpExporterBuilder.build()).build()
            )
        }

        // 创建跟踪提供者
        val tracerProvider = tracerProviderBuilder.build()

        // 创建传播器
        val propagators = mutableListOf<TextMapPropagator>()
        if (propagation.w3c) {
            propagators.add(W3CTraceContextPropagator.getInstance())
        }
        if (propagation.b3) {
            // 添加 B3 传播器
            try {
                val b3Propagator = Class.forName("io.opentelemetry.extension.trace.propagation.B3Propagator")
                    .getMethod("injectingMultiHeaders")
                    .invoke(null) as TextMapPropagator
                propagators.add(b3Propagator)
            } catch (e: Exception) {
                System.err.println("Failed to load B3 propagator: ${e.message}")
            }
        }
        if (propagation.jaeger) {
            // 添加 Jaeger 传播器
            try {
                val jaegerPropagator = Class.forName("io.opentelemetry.extension.trace.propagation.JaegerPropagator")
                    .getMethod("getInstance")
                    .invoke(null) as TextMapPropagator
                propagators.add(jaegerPropagator)
            } catch (e: Exception) {
                System.err.println("Failed to load Jaeger propagator: ${e.message}")
            }
        }

        // 创建 OpenTelemetry SDK
        val openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(TextMapPropagator.composite(propagators)))
            .build()

        // 创建跟踪器
        return OpenTelemetryTracer(
            openTelemetry = openTelemetry,
            instrumentationName = serviceName,
            instrumentationVersion = serviceVersion
        )
    }
}

/**
 * 空操作跟踪器实现。
 */
class NoopTracer : Tracer {
    override fun createSpan(name: String, kind: OTelSpanKind, attributes: Map<String, Any>): TraceSpan {
        return NoopTraceSpan(name)
    }

    override fun getCurrentSpan(): TraceSpan? {
        return null
    }

    override fun getCurrentContext(): TraceContext {
        return NoopTraceContext()
    }

    override fun <C> extractContext(carrier: C, getter: OTelTextMapGetter<C>): TraceContext {
        return NoopTraceContext()
    }

    override fun <C> injectContext(context: TraceContext, carrier: C, setter: OTelTextMapSetter<C>) {
        // 空操作
    }
}

/**
 * 空操作跟踪范围实现。
 *
 * @property name 范围名称
 */
class NoopTraceSpan(
    private val name: String
) : TraceSpan {
    override fun getName(): String {
        return name
    }

    override fun setAttribute(key: String, value: String): TraceSpan {
        return this
    }

    override fun setAttribute(key: String, value: Long): TraceSpan {
        return this
    }

    override fun setAttribute(key: String, value: Double): TraceSpan {
        return this
    }

    override fun setAttribute(key: String, value: Boolean): TraceSpan {
        return this
    }

    override fun addEvent(name: String, attributes: Map<String, Any>): TraceSpan {
        return this
    }

    override fun setSuccess(): TraceSpan {
        return this
    }

    override fun setError(description: String): TraceSpan {
        return this
    }

    override fun recordException(exception: Throwable): TraceSpan {
        return this
    }

    override fun end() {
        // 空操作
    }

    override fun getContext(): TraceContext {
        return NoopTraceContext()
    }

    override fun makeCurrent(): OTelScope {
        return NoopScope()
    }

    override fun close() {
        // 空操作
    }
}

/**
 * 空操作跟踪上下文实现。
 */
class NoopTraceContext : TraceContext {
    override fun getUnderlyingContext(): Any {
        return this
    }
}

/**
 * 空操作作用域实现。
 */
class NoopScope : OTelScope {
    override fun close() {
        // 空操作
    }
}
