package ai.kastrax.observability.dashboard.grafana

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Grafana客户端。
 * 用于与Grafana API交互。
 *
 * @property baseUrl Grafana基础URL
 * @property apiKey Grafana API密钥
 * @property timeout 超时时间
 */
class GrafanaClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val timeout: Duration = Duration.ofSeconds(10)
) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()

    /**
     * 创建仪表板。
     *
     * @param dashboard Grafana仪表板
     * @return 响应内容
     */
    fun createDashboard(dashboard: GrafanaDashboard): String {
        val config = dashboard.exportConfig()
        return post("/api/dashboards/db", config)
    }

    /**
     * 获取仪表板。
     *
     * @param uid 仪表板UID
     * @return 响应内容
     */
    fun getDashboard(uid: String): String {
        return get("/api/dashboards/uid/$uid")
    }

    /**
     * 删除仪表板。
     *
     * @param uid 仪表板UID
     * @return 响应内容
     */
    fun deleteDashboard(uid: String): String {
        return delete("/api/dashboards/uid/$uid")
    }

    /**
     * 获取所有仪表板。
     *
     * @return 响应内容
     */
    fun getAllDashboards(): String {
        return get("/api/search?type=dash-db")
    }

    /**
     * 获取数据源。
     *
     * @return 响应内容
     */
    fun getDataSources(): String {
        return get("/api/datasources")
    }

    /**
     * 发送GET请求。
     *
     * @param path 请求路径
     * @return 响应内容
     */
    private fun get(path: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .GET()
            .timeout(timeout)
            .build()

        return sendRequest(request)
    }

    /**
     * 发送POST请求。
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 响应内容
     */
    private fun post(path: String, body: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(timeout)
            .build()

        return sendRequest(request)
    }

    /**
     * 发送DELETE请求。
     *
     * @param path 请求路径
     * @return 响应内容
     */
    private fun delete(path: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .DELETE()
            .timeout(timeout)
            .build()

        return sendRequest(request)
    }

    /**
     * 发送请求并处理响应。
     *
     * @param request HTTP请求
     * @return 响应内容
     */
    private fun sendRequest(request: HttpRequest): String {
        try {
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() >= 400) {
                throw GrafanaException("Grafana API error: ${response.statusCode()} - ${response.body()}")
            }
            return response.body()
        } catch (e: Exception) {
            if (e is GrafanaException) throw e
            throw GrafanaException("Failed to communicate with Grafana: ${e.message}", e)
        }
    }
}

/**
 * Grafana异常。
 *
 * @param message 异常消息
 * @param cause 异常原因
 */
class GrafanaException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
