package ai.kastrax.native.config

import kotlinx.serialization.json.Json

/**
 * 加载配置
 */
expect fun loadConfig(): AppConfig

/**
 * 保存配置
 */
expect fun saveConfig(config: AppConfig)

/**
 * 将配置序列化为JSON字符串
 */
fun serializeConfig(config: AppConfig): String {
    val json = Json { 
        prettyPrint = true 
        encodeDefaults = true
    }
    return json.encodeToString(AppConfig.serializer(), config)
}

/**
 * 从JSON字符串反序列化配置
 */
fun deserializeConfig(jsonString: String): AppConfig {
    val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    return json.decodeFromString(AppConfig.serializer(), jsonString)
}
