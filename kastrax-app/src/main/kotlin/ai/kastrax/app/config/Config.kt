package ai.kastrax.app.config

import com.google.gson.Gson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileReader
import java.util.Properties

private val logger = KotlinLogging.logger {}

/**
 * 应用程序配置。
 */
data class AppConfig(
    val appName: String = "KastraX App",
    val apiKeys: Map<String, String> = emptyMap(),
    val serverConfig: ServerConfig = ServerConfig(),
    val agentConfig: AgentConfig = AgentConfig(),
    val toolConfig: ToolConfig = ToolConfig()
)

/**
 * 服务器配置。
 */
data class ServerConfig(
    val port: Int = 8080,
    val host: String = "localhost",
    val enableCors: Boolean = true
)

/**
 * 代理配置。
 */
data class AgentConfig(
    val defaultModel: String = "gpt-4",
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000
)

/**
 * 工具配置。
 */
data class ToolConfig(
    val enabledTools: List<String> = listOf("calculator", "weather", "search"),
    val toolTimeout: Long = 30000 // 毫秒
)

/**
 * 加载应用程序配置。
 * 
 * 配置加载顺序：
 * 1. 默认配置
 * 2. 配置文件 (config.json)
 * 3. 环境变量
 */
fun loadConfig(): AppConfig {
    // 默认配置
    var config = AppConfig()
    
    try {
        // 从配置文件加载
        val configFile = File("config.json")
        if (configFile.exists()) {
            logger.info { "从 config.json 加载配置" }
            val gson = Gson()
            config = gson.fromJson(FileReader(configFile), AppConfig::class.java)
        } else {
            logger.info { "配置文件 config.json 不存在，使用默认配置" }
        }
        
        // 从环境变量加载 API 密钥
        val apiKeys = mutableMapOf<String, String>()
        apiKeys.putAll(config.apiKeys)
        
        // 常见的 API 密钥环境变量
        val keyMappings = mapOf(
            "OPENAI_API_KEY" to "openai",
            "ANTHROPIC_API_KEY" to "anthropic",
            "GOOGLE_API_KEY" to "google",
            "AZURE_OPENAI_API_KEY" to "azure_openai"
        )
        
        for ((envVar, keyName) in keyMappings) {
            System.getenv(envVar)?.let { apiKey ->
                logger.info { "从环境变量加载 $keyName API 密钥" }
                apiKeys[keyName] = apiKey
            }
        }
        
        // 更新配置
        config = config.copy(apiKeys = apiKeys)
        
        // 从 .env 文件加载（如果存在）
        val envFile = File(".env")
        if (envFile.exists()) {
            logger.info { "从 .env 文件加载环境变量" }
            val properties = Properties()
            FileReader(envFile).use { properties.load(it) }
            
            for ((envVar, keyName) in keyMappings) {
                properties.getProperty(envVar)?.let { apiKey ->
                    logger.info { "从 .env 文件加载 $keyName API 密钥" }
                    apiKeys[keyName] = apiKey
                }
            }
            
            // 更新配置
            config = config.copy(apiKeys = apiKeys)
        }
        
    } catch (e: Exception) {
        logger.error(e) { "加载配置时发生错误，使用默认配置" }
    }
    
    return config
}
