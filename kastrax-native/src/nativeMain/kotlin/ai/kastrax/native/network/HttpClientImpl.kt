package ai.kastrax.native.network

import io.ktor.client.*
import io.ktor.client.engine.curl.*

/**
 * 创建Native平台特定的HTTP客户端
 */
actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Curl) {
        // 简化配置，避免使用不兼容的属性
    }
}
