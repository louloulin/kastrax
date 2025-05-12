package ai.kastrax.codebase.semantic.parser

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Modifier
import chapi.domain.core.CodeContainer
import chapi.ast.kotlinast.KotlinAnalyser
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

/**
 * 基于 Chapi 的 Kotlin 代码解析器
 *
 * 使用 Chapi 的 KotlinAnalyser 解析 Kotlin 代码文件
 */
class ChapiKotlinCodeParser : ChapiCodeParser() {

    /**
     * 使用 Chapi 解析 Kotlin 代码
     *
     * @param content 代码内容
     * @return Chapi 代码容器
     */
    override fun parseCodeByChapi(content: String): CodeContainer {
        try {
            val analyser = KotlinAnalyser()
            return analyser.analysis(content, "")
        } catch (e: Exception) {
            logger.error(e) { "使用 Chapi 解析 Kotlin 代码时出错" }
            // 返回空的代码容器
            return CodeContainer()
        }
    }

    /**
     * 获取支持的文件扩展名
     *
     * @return 支持的文件扩展名集合
     */
    override fun getSupportedExtensions(): Set<String> {
        return setOf("kt", "kts")
    }

    /**
     * 获取语言名称
     *
     * @return 语言名称
     */
    override fun getLanguageName(): String {
        return "kotlin"
    }

    /**
     * 增强的 Kotlin 特定解析
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 代码元素（文件级别）
     */
    override fun parseFile(filePath: Path, content: String): CodeElement {
        // 首先使用基类的解析方法
        val baseElement = super.parseFile(filePath, content)

        try {
            // 增强 Kotlin 特定的解析
            enhanceKotlinSpecificFeatures(baseElement, content)

            return baseElement
        } catch (e: Exception) {
            logger.error(e) { "增强 Kotlin 特定解析时出错: $filePath" }
            return baseElement
        }
    }

    /**
     * 增强 Kotlin 特定特性
     *
     * @param element 代码元素
     * @param content 文件内容
     */
    private fun enhanceKotlinSpecificFeatures(element: CodeElement, content: String) {
        // 检测顶级函数和属性
        detectTopLevelFunctionsAndProperties(element)

        // 检测扩展函数和属性
        detectExtensionFunctionsAndProperties(element)

        // 检测委托属性
        detectDelegatedProperties(element)

        // 检测协程相关特性
        detectCoroutineFeatures(element)

        // 检测DSL标记
        detectDslMarkers(element)
    }

    /**
     * 检测顶级函数和属性
     *
     * @param element 代码元素
     */
    private fun detectTopLevelFunctionsAndProperties(element: CodeElement) {
        // 查找文件中的顶级函数和属性
        element.children.forEach { child ->
            if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.METHOD) {
                child.metadata["isTopLevel"] = true
            } else if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.PROPERTY) {
                child.metadata["isTopLevel"] = true
            }
        }
    }

    /**
     * 检测扩展函数和属性
     *
     * @param element 代码元素
     */
    private fun detectExtensionFunctionsAndProperties(element: CodeElement) {
        // 递归检查所有方法和属性
        element.getAllChildren().forEach { child ->
            if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.METHOD) {
                // 检查方法是否是扩展函数
                val receiverType = child.metadata["receiverType"] as? String
                if (!receiverType.isNullOrEmpty()) {
                    child.metadata["isExtensionFunction"] = true
                    child.metadata["extensionReceiverType"] = receiverType
                }
            } else if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.PROPERTY) {
                // 检查属性是否是扩展属性
                val receiverType = child.metadata["receiverType"] as? String
                if (!receiverType.isNullOrEmpty()) {
                    child.metadata["isExtensionProperty"] = true
                    child.metadata["extensionReceiverType"] = receiverType
                }
            }
        }
    }

    /**
     * 检测委托属性
     *
     * @param element 代码元素
     */
    private fun detectDelegatedProperties(element: CodeElement) {
        // 递归检查所有属性
        element.getAllChildren().forEach { child ->
            if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.PROPERTY) {
                // 检查属性是否是委托属性
                val initializer = child.metadata["initializer"] as? String
                if (initializer?.contains("by ") == true) {
                    child.metadata["isDelegated"] = true
                    // 尝试提取委托表达式
                    val delegateExpression = initializer.substringAfter("by ").trim()
                    child.metadata["delegateExpression"] = delegateExpression
                }
            }
        }
    }

    /**
     * 检测协程相关特性
     *
     * @param element 代码元素
     */
    private fun detectCoroutineFeatures(element: CodeElement) {
        // 递归检查所有方法
        element.getAllChildren().forEach { child ->
            if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.METHOD) {
                // 检查方法是否是挂起函数
                if (child.modifiers.contains(ai.kastrax.codebase.semantic.model.Modifier.SUSPEND)) {
                    child.metadata["isSuspendFunction"] = true
                }
            }
        }
    }

    /**
     * 检测DSL标记
     *
     * @param element 代码元素
     */
    private fun detectDslMarkers(element: CodeElement) {
        // 递归检查所有类和接口
        element.getAllChildren().forEach { child ->
            if (child.type == ai.kastrax.codebase.semantic.model.CodeElementType.CLASS ||
                child.type == ai.kastrax.codebase.semantic.model.CodeElementType.INTERFACE) {
                // 检查是否有 @DslMarker 注解
                val annotations = child.metadata["annotations"] as? List<*>
                if (annotations?.any { it.toString().contains("DslMarker") } == true) {
                    child.metadata["isDslMarker"] = true
                }
            }
        }
    }
}
