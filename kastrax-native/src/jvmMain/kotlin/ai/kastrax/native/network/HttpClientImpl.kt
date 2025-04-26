package ai.kastrax.native.network

import io.ktor.client.*
import io.ktor.client.engine.java.*

/**
 * 创建JVM平台特定的HTTP客户端
 */
actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Java) {
        engine {
            // 配置Java引擎
            pipelining = true
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
    }
}
