package ai.kastrax.native.config

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 配置文件路径
 */
private const val CONFIG_FILE_PATH = "config/kastrax-native.json"

/**
 * 加载JVM平台的配置
 */
actual fun loadConfig(): AppConfig {
    logger.info { "加载配置文件: $CONFIG_FILE_PATH" }
    
    val configFile = File(CONFIG_FILE_PATH)
    
    return if (configFile.exists()) {
        try {
            val jsonString = configFile.readText()
            deserializeConfig(jsonString)
        } catch (e: Exception) {
            logger.error(e) { "读取配置文件失败，使用默认配置" }
            val defaultConfig = AppConfig()
            saveConfig(defaultConfig)
            defaultConfig
        }
    } else {
        logger.info { "配置文件不存在，创建默认配置" }
        val defaultConfig = AppConfig()
        saveConfig(defaultConfig)
        defaultConfig
    }
}

/**
 * 保存JVM平台的配置
 */
actual fun saveConfig(config: AppConfig) {
    try {
        val configDir = File("config")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        
        val jsonString = serializeConfig(config)
        File(CONFIG_FILE_PATH).writeText(jsonString)
        logger.info { "配置已保存到: $CONFIG_FILE_PATH" }
    } catch (e: Exception) {
        logger.error(e) { "保存配置文件失败" }
    }
}
