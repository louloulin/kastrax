package ai.kastrax.codebase.semantic.parser

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Chapi 代码解析器注册器
 *
 * 用于注册所有 Chapi 代码解析器
 */
object ChapiCodeParserRegistrar {
    /**
     * 注册所有 Chapi 代码解析器
     */
    fun registerAllParsers() {
        logger.info { "注册 Chapi 代码解析器..." }
        
        try {
            // 注册 Java 代码解析器
            CodeParserFactory.registerParser(ChapiJavaCodeParser())
            logger.debug { "已注册 Java 代码解析器" }
            
            // 注册 Kotlin 代码解析器
            CodeParserFactory.registerParser(ChapiKotlinCodeParser())
            logger.debug { "已注册 Kotlin 代码解析器" }
            
            // 注册 Python 代码解析器
            CodeParserFactory.registerParser(ChapiPythonCodeParser())
            logger.debug { "已注册 Python 代码解析器" }
            
            // 注册 TypeScript 代码解析器
            CodeParserFactory.registerParser(ChapiTypeScriptCodeParser())
            logger.debug { "已注册 TypeScript 代码解析器" }
            
            // 注册 Go 代码解析器
            CodeParserFactory.registerParser(ChapiGoCodeParser())
            logger.debug { "已注册 Go 代码解析器" }
            
            logger.info { "所有 Chapi 代码解析器注册完成" }
        } catch (e: Exception) {
            logger.error(e) { "注册 Chapi 代码解析器时发生错误" }
        }
    }
}
