package ai.kastrax.codebase.semantic.parser

import chapi.domain.core.CodeContainer
import chapi.ast.typescriptast.TypeScriptAnalyser

/**
 * 基于 Chapi 的 TypeScript/JavaScript 代码解析器
 *
 * 使用 Chapi 的 TypeScriptAnalyser 解析 TypeScript 和 JavaScript 代码文件
 */
class ChapiTypeScriptCodeParser : ChapiCodeParser() {

    /**
     * 使用 Chapi 解析 TypeScript/JavaScript 代码
     *
     * @param content 代码内容
     * @return Chapi 代码容器
     */
    override fun parseCodeByChapi(content: String): CodeContainer {
        val analyser = TypeScriptAnalyser()
        return analyser.analysis(content, "")
    }

    /**
     * 获取支持的文件扩展名
     *
     * @return 支持的文件扩展名集合
     */
    override fun getSupportedExtensions(): Set<String> {
        return setOf("ts", "tsx", "js", "jsx")
    }

    /**
     * 获取语言名称
     *
     * @return 语言名称
     */
    override fun getLanguageName(): String {
        return "typescript"
    }
}
