package ai.kastrax.codebase.semantic.parser

// TODO: 暂时注释掉，等待依赖问题解决

// 空实现以避免语法错误
class ChapiKotlinCodeParser : AbstractCodeParser() {
    override fun parseFile(filePath: java.nio.file.Path, content: String): ai.kastrax.codebase.semantic.model.CodeElement {
        return createFileElement(filePath, content)
    }

    override fun getSupportedExtensions(): Set<String> {
        return setOf("kt", "kts")
    }

    override fun getLanguageName(): String {
        return "kotlin"
    }
}

/*
import chapi.domain.core.CodeContainer
import chapi.ast.kotlinast.KotlinAnalyser

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
        val analyser = KotlinAnalyser()
        return analyser.analysis(content, "")
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
}
*/
