package ai.kastrax.examples.mcp

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 翻译服务 MCP 应用案例
 * 
 * 这个示例展示了如何创建一个提供多语言翻译功能的 MCP 服务器，
 * 以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。
 */
fun main() = runBlocking {
    println("启动翻译服务 MCP 应用案例...")
    
    // 启动服务器
    val serverJob = launch {
        val server = createTranslationServer()
        server.startSSE(port = 8083)
        println("翻译服务 MCP 服务器已启动在端口 8083")
        
        // 保持服务器运行
        try {
            while (true) {
                delay(1000)
            }
        } finally {
            server.stop()
            println("翻译服务 MCP 服务器已停止")
        }
    }
    
    // 等待服务器启动
    delay(2000)
    
    // 创建并使用客户端
    val client = createTranslationClient()
    
    try {
        // 连接到服务器
        println("连接到翻译服务 MCP 服务器...")
        client.connect()
        println("已连接到翻译服务 MCP 服务器")
        
        // 获取服务器能力
        val hasTools = client.supportsCapability("tools")
        println("服务器支持工具: $hasTools")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 测试翻译功能
        val testTexts = listOf(
            "你好，世界！" to "zh",
            "Hello, world!" to "en",
            "こんにちは、世界！" to "ja",
            "Bonjour le monde!" to "fr",
            "Hallo Welt!" to "de"
        )
        
        // 翻译到不同语言
        val targetLanguages = listOf("en", "zh", "ja", "fr", "de", "es", "ru")
        
        for ((text, sourceLanguage) in testTexts) {
            println("\n原文 ($sourceLanguage): $text")
            
            for (targetLanguage in targetLanguages) {
                if (sourceLanguage != targetLanguage) {
                    println("翻译到 $targetLanguage...")
                    val translationResult = client.callTool("translate", mapOf(
                        "text" to text,
                        "source_language" to sourceLanguage,
                        "target_language" to targetLanguage
                    ))
                    println("翻译结果 ($targetLanguage): $translationResult")
                }
            }
        }
        
        // 测试语言检测功能
        val textsToDetect = listOf(
            "这是一段中文文本，用于测试语言检测功能。",
            "This is an English text for testing language detection.",
            "これは言語検出をテストするための日本語のテキストです。",
            "Ceci est un texte français pour tester la détection de langue.",
            "Dies ist ein deutscher Text zum Testen der Spracherkennung."
        )
        
        println("\n测试语言检测功能:")
        for (text in textsToDetect) {
            println("\n文本: $text")
            val detectionResult = client.callTool("detect_language", mapOf("text" to text))
            println("检测结果: $detectionResult")
        }
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与翻译服务 MCP 服务器的连接")
        
        // 停止服务器
        serverJob.cancel()
    }
}

/**
 * 创建翻译服务 MCP 服务器
 */
private fun createTranslationServer() = mcpServer {
    name("TranslationMCPServer")
    version("1.0.0")
    
    // 添加翻译工具
    tool {
        name = "translate"
        description = "将文本从一种语言翻译到另一种语言"
        
        // 添加参数
        parameters {
            parameter {
                name = "text"
                description = "要翻译的文本"
                type = "string"
                required = true
            }
            
            parameter {
                name = "source_language"
                description = "源语言代码（如 en, zh, ja, fr, de 等）"
                type = "string"
                required = true
            }
            
            parameter {
                name = "target_language"
                description = "目标语言代码（如 en, zh, ja, fr, de 等）"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val text = params["text"] as? String ?: ""
            val sourceLanguage = params["source_language"] as? String ?: "en"
            val targetLanguage = params["target_language"] as? String ?: "en"
            
            println("执行translate工具，文本: $text, 源语言: $sourceLanguage, 目标语言: $targetLanguage")
            
            // 模拟翻译
            val translatedText = simulateTranslation(text, sourceLanguage, targetLanguage)
            
            // 返回翻译结果
            translatedText
        }
    }
    
    // 添加语言检测工具
    tool {
        name = "detect_language"
        description = "检测文本的语言"
        
        // 添加参数
        parameters {
            parameter {
                name = "text"
                description = "要检测语言的文本"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val text = params["text"] as? String ?: ""
            
            println("执行detect_language工具，文本: $text")
            
            // 模拟语言检测
            val detectionResult = simulateLanguageDetection(text)
            
            // 格式化返回结果
            """
            |检测到的语言: ${detectionResult.language}
            |语言名称: ${detectionResult.languageName}
            |置信度: ${detectionResult.confidence}%
            """.trimMargin()
        }
    }
    
    // 添加支持的语言列表工具
    tool {
        name = "supported_languages"
        description = "获取支持的语言列表"
        
        // 设置执行函数
        handler { _ ->
            println("执行supported_languages工具")
            
            // 获取支持的语言列表
            val languages = getSupportedLanguages()
            
            // 格式化返回结果
            val languagesList = languages.entries.joinToString("\n") { (code, name) ->
                "- $code: $name"
            }
            
            """
            |支持的语言列表:
            |
            |$languagesList
            """.trimMargin()
        }
    }
}

/**
 * 创建翻译服务 MCP 客户端
 */
private fun createTranslationClient() = mcpClient {
    name("TranslationMCPClient")
    version("1.0.0")
    
    server {
        sse {
            url = "http://localhost:8083"
        }
    }
}

/**
 * 语言检测结果类
 */
private data class LanguageDetectionResult(
    val language: String,
    val languageName: String,
    val confidence: Double
)

/**
 * 获取支持的语言列表
 */
private fun getSupportedLanguages(): Map<String, String> {
    return mapOf(
        "en" to "英语",
        "zh" to "中文",
        "ja" to "日语",
        "ko" to "韩语",
        "fr" to "法语",
        "de" to "德语",
        "es" to "西班牙语",
        "it" to "意大利语",
        "ru" to "俄语",
        "pt" to "葡萄牙语",
        "ar" to "阿拉伯语",
        "hi" to "印地语",
        "th" to "泰语",
        "vi" to "越南语"
    )
}

/**
 * 模拟翻译
 */
private fun simulateTranslation(text: String, sourceLanguage: String, targetLanguage: String): String {
    // 这里只是模拟翻译，实际应用中应该调用真实的翻译API
    val translations = mapOf(
        "Hello, world!" to mapOf(
            "zh" to "你好，世界！",
            "ja" to "こんにちは、世界！",
            "fr" to "Bonjour le monde!",
            "de" to "Hallo Welt!",
            "es" to "¡Hola, mundo!",
            "ru" to "Привет, мир!"
        ),
        "你好，世界！" to mapOf(
            "en" to "Hello, world!",
            "ja" to "こんにちは、世界！",
            "fr" to "Bonjour le monde!",
            "de" to "Hallo Welt!",
            "es" to "¡Hola, mundo!",
            "ru" to "Привет, мир!"
        ),
        "こんにちは、世界！" to mapOf(
            "en" to "Hello, world!",
            "zh" to "你好，世界！",
            "fr" to "Bonjour le monde!",
            "de" to "Hallo Welt!",
            "es" to "¡Hola, mundo!",
            "ru" to "Привет, мир!"
        ),
        "Bonjour le monde!" to mapOf(
            "en" to "Hello, world!",
            "zh" to "你好，世界！",
            "ja" to "こんにちは、世界！",
            "de" to "Hallo Welt!",
            "es" to "¡Hola, mundo!",
            "ru" to "Привет, мир!"
        ),
        "Hallo Welt!" to mapOf(
            "en" to "Hello, world!",
            "zh" to "你好，世界！",
            "ja" to "こんにちは、世界！",
            "fr" to "Bonjour le monde!",
            "es" to "¡Hola, mundo!",
            "ru" to "Привет, мир!"
        )
    )
    
    // 如果有预定义的翻译，则使用它
    if (translations.containsKey(text) && translations[text]?.containsKey(targetLanguage) == true) {
        return translations[text]!![targetLanguage]!!
    }
    
    // 否则生成一个模拟的翻译
    return when (targetLanguage) {
        "en" -> "[英语翻译] $text"
        "zh" -> "[中文翻译] $text"
        "ja" -> "[日语翻译] $text"
        "ko" -> "[韩语翻译] $text"
        "fr" -> "[法语翻译] $text"
        "de" -> "[德语翻译] $text"
        "es" -> "[西班牙语翻译] $text"
        "it" -> "[意大利语翻译] $text"
        "ru" -> "[俄语翻译] $text"
        "pt" -> "[葡萄牙语翻译] $text"
        "ar" -> "[阿拉伯语翻译] $text"
        "hi" -> "[印地语翻译] $text"
        "th" -> "[泰语翻译] $text"
        "vi" -> "[越南语翻译] $text"
        else -> "[未知语言翻译] $text"
    }
}

/**
 * 模拟语言检测
 */
private fun simulateLanguageDetection(text: String): LanguageDetectionResult {
    // 这里只是模拟语言检测，实际应用中应该调用真实的语言检测API
    val languagePatterns = mapOf(
        "zh" to Regex("[\u4e00-\u9fa5]"),
        "ja" to Regex("[\u3040-\u309f\u30a0-\u30ff]"),
        "ko" to Regex("[\uac00-\ud7af\u1100-\u11ff]"),
        "ru" to Regex("[\u0400-\u04ff]"),
        "ar" to Regex("[\u0600-\u06ff]"),
        "th" to Regex("[\u0e00-\u0e7f]"),
        "hi" to Regex("[\u0900-\u097f]")
    )
    
    // 检查是否匹配特定语言的字符
    for ((language, pattern) in languagePatterns) {
        if (pattern.containsMatchIn(text)) {
            val languageName = getSupportedLanguages()[language] ?: "未知语言"
            return LanguageDetectionResult(language, languageName, 95.0)
        }
    }
    
    // 如果没有匹配特定字符，则根据常见单词判断
    val lowerText = text.lowercase()
    
    return when {
        lowerText.contains("bonjour") || lowerText.contains("le monde") || lowerText.contains("français") -> 
            LanguageDetectionResult("fr", "法语", 90.0)
        lowerText.contains("hallo") || lowerText.contains("welt") || lowerText.contains("deutsch") -> 
            LanguageDetectionResult("de", "德语", 90.0)
        lowerText.contains("hola") || lowerText.contains("mundo") || lowerText.contains("español") -> 
            LanguageDetectionResult("es", "西班牙语", 90.0)
        lowerText.contains("ciao") || lowerText.contains("mondo") || lowerText.contains("italiano") -> 
            LanguageDetectionResult("it", "意大利语", 90.0)
        else -> 
            // 默认假设是英语
            LanguageDetectionResult("en", "英语", 85.0)
    }
}
