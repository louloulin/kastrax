package actor.proto.metrics.prometheus

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.HTTPServer
import java.io.Closeable
import java.net.InetSocketAddress

/**
 * Prometheus 度量导出器
 * 启动一个 HTTP 服务器来暴露 Prometheus 度量
 */
class PrometheusExporter(
    private val registry: CollectorRegistry,
    private val host: String = "0.0.0.0",
    private val port: Int = 9090
) : Closeable {
    private var server: HTTPServer? = null
    
    /**
     * 启动 HTTP 服务器
     */
    fun start() {
        if (server == null) {
            server = HTTPServer(InetSocketAddress(host, port), registry, true)
            println("Prometheus metrics available at http://$host:$port/metrics")
        }
    }
    
    /**
     * 停止 HTTP 服务器
     */
    override fun close() {
        server?.stop()
        server = null
    }
}
