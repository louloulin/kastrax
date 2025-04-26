package ai.kastrax.native.config

import kotlinx.cinterop.*
import platform.posix.*
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * 配置文件路径
 */
private const val CONFIG_FILE_PATH = "config/kastrax-native.json"

/**
 * 加载Native平台的配置
 */
@OptIn(ExperimentalForeignApi::class)
actual fun loadConfig(): AppConfig {
    println("加载配置文件: $CONFIG_FILE_PATH")
    
    // 检查配置目录是否存在
    val configDir = "config"
    val dirExists = access(configDir, F_OK) == 0
    
    if (!dirExists) {
        println("配置目录不存在，创建目录")
        mkdir(configDir, 0x1FFu.toUShort()) // 0x1FF = 0777 权限
    }
    
    // 检查配置文件是否存在
    val fileExists = access(CONFIG_FILE_PATH, F_OK) == 0
    
    return if (fileExists) {
        try {
            val jsonString = readTextFile(CONFIG_FILE_PATH)
            if (jsonString.isNotEmpty()) {
                deserializeConfig(jsonString)
            } else {
                println("配置文件为空，使用默认配置")
                val defaultConfig = AppConfig()
                saveConfig(defaultConfig)
                defaultConfig
            }
        } catch (e: Exception) {
            println("读取配置文件失败，使用默认配置: ${e.message}")
            val defaultConfig = AppConfig()
            saveConfig(defaultConfig)
            defaultConfig
        }
    } else {
        println("配置文件不存在，创建默认配置")
        val defaultConfig = AppConfig()
        saveConfig(defaultConfig)
        defaultConfig
    }
}

/**
 * 保存Native平台的配置
 */
@OptIn(ExperimentalForeignApi::class)
actual fun saveConfig(config: AppConfig) {
    try {
        val jsonString = serializeConfig(config)
        writeTextFile(CONFIG_FILE_PATH, jsonString)
        println("配置已保存到: $CONFIG_FILE_PATH")
    } catch (e: Exception) {
        println("保存配置文件失败: ${e.message}")
    }
}

/**
 * 读取文本文件
 */
@OptIn(ExperimentalForeignApi::class)
private fun readTextFile(filePath: String): String {
    val file = fopen(filePath, "r") ?: throw IllegalArgumentException("无法打开文件: $filePath")
    
    try {
        val buffer = StringBuilder()
        val bufferSize = 1024
        val readBuffer = ByteArray(bufferSize)
        
        memScoped {
            val readBufferPtr = allocArray<ByteVar>(bufferSize)
            
            while (true) {
                val bytesRead = fread(readBufferPtr, 1.toULong(), bufferSize.toULong(), file)
                if (bytesRead == 0uL) break
                
                for (i in 0 until bytesRead.toInt()) {
                    buffer.append(readBufferPtr[i].toInt().toChar())
                }
            }
        }
        
        return buffer.toString()
    } finally {
        fclose(file)
    }
}

/**
 * 写入文本文件
 */
@OptIn(ExperimentalForeignApi::class)
private fun writeTextFile(filePath: String, content: String) {
    val file = fopen(filePath, "w") ?: throw IllegalArgumentException("无法创建文件: $filePath")
    
    try {
        memScoped {
            val result = fputs(content, file)
            if (result == EOF) {
                throw IOException("写入文件失败: $filePath")
            }
        }
    } finally {
        fclose(file)
    }
}

/**
 * IO异常
 */
class IOException(message: String) : Exception(message)
