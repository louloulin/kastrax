package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthCheck
import ai.kastrax.observability.health.HealthCheckType
import ai.kastrax.observability.health.HealthResult
import ai.kastrax.observability.health.HealthStatus
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration

/**
 * HTTP 端点健康检查。
 * 检查指定 URL 的 HTTP 端点是否可访问。
 *
 * @property url 要检查的 URL
 * @property timeout 超时时间
 * @property expectedStatusCode 预期的 HTTP 状态码
 */
class HttpEndpointHealthCheck(
    private val url: String,
    private val timeout: Duration = Duration.ofSeconds(5),
    private val expectedStatusCode: Int = 200
) : HealthCheck {
    override fun getName(): String = "http_endpoint_${url.replace(Regex("[^a-zA-Z0-9]"), "_")}"

    override fun getType(): HealthCheckType = HealthCheckType.DEPENDENCY

    override fun check(): HealthResult {
        val startTime = System.currentTimeMillis()
        var connection: HttpURLConnection? = null

        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = timeout.toMillis().toInt()
            connection.readTimeout = timeout.toMillis().toInt()
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true

            val statusCode = connection.responseCode
            val responseTime = System.currentTimeMillis() - startTime

            val details = mapOf(
                "url" to url,
                "status_code" to statusCode.toString(),
                "response_time_ms" to responseTime.toString()
            )

            return when {
                statusCode == expectedStatusCode -> HealthResult(HealthStatus.UP, details)
                statusCode in 200..399 -> HealthResult(
                    HealthStatus.DEGRADED,
                    details,
                    RuntimeException("Unexpected status code: $statusCode, expected: $expectedStatusCode")
                )
                else -> HealthResult(
                    HealthStatus.DOWN,
                    details,
                    RuntimeException("HTTP endpoint is down with status code: $statusCode")
                )
            }
        } catch (e: Exception) {
            val responseTime = System.currentTimeMillis() - startTime
            val details = mapOf(
                "url" to url,
                "response_time_ms" to responseTime.toString()
            )
            return HealthResult(HealthStatus.DOWN, details, e)
        } finally {
            connection?.disconnect()
        }
    }
}
