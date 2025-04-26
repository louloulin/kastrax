package ai.kastrax.native.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 应用程序配置
 */
@Serializable
data class AppConfig(
    val appName: String = "KastraX Native",
    val version: String = "0.1.0",
    val apiKeys: ApiKeys = ApiKeys(),
    val logging: LoggingConfig = LoggingConfig()
)

/**
 * API密钥配置
 */
@Serializable
data class ApiKeys(
    val deepseek: String = "",
    val anthropic: String = "",
    val openai: String = ""
)

/**
 * 日志配置
 */
@Serializable
data class LoggingConfig(
    val level: String = "INFO",
    val enableConsole: Boolean = true,
    val enableFile: Boolean = false,
    val filePath: String = "logs/kastrax-native.log"
)
