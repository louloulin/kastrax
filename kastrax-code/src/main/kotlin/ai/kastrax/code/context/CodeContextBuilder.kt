package ai.kastrax.code.context

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.Location
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 代码上下文构建器
 *
 * 根据当前编辑位置和查询意图构建上下文
 */
class CodeContextBuilder(
    private val project: Project,
    private val contextEngine: CodeContextEngine
) : KastraXCodeBase(component = "CODE_CONTEXT_BUILDER") {

    // 使用父类的logger

    /**
     * 从编辑器构建上下文
     *
     * @param editor 编辑器
     * @param maxResults 最大结果数量
     * @param includeRelated 是否包含相关元素
     * @return 上下文
     */
    suspend fun buildFromEditor(
        editor: Editor,
        maxResults: Int = 10,
        includeRelated: Boolean = true
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info("从编辑器构建上下文")

            // 获取当前文件
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (psiFile == null) {
                logger.warn("无法获取当前文件")
                return@withContext Context(elements = emptyList(), query = "")
            }

            // 获取当前位置
            val offset = editor.caretModel.offset
            val position = getLocationFromOffset(editor, offset)

            // 获取文件路径
            val filePath = getFilePath(psiFile)

            // 获取编辑上下文
            return@withContext contextEngine.getEditContext(
                filePath = filePath,
                position = position,
                maxResults = maxResults,
                minScore = 0.0
            )
        } catch (e: Exception) {
            logger.error("从编辑器构建上下文时出错", e)
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 从文件构建上下文
     *
     * @param file 文件
     * @param maxResults 最大结果数量
     * @return 上下文
     */
    suspend fun buildFromFile(
        file: PsiFile,
        maxResults: Int = 10
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info("从文件构建上下文: ${file.name}")

            // 获取文件路径
            val filePath = getFilePath(file)

            // 获取文件上下文
            return@withContext contextEngine.getFileContext(filePath, maxResults)
        } catch (e: Exception) {
            logger.error("从文件构建上下文时出错: ${file.name}", e)
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 从元素构建上下文
     *
     * @param element 元素
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @return 上下文
     */
    suspend fun buildFromElement(
        element: PsiElement,
        maxResults: Int = 10,
        minScore: Double = 0.0
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info("从元素构建上下文: ${element.text}")

            // 获取元素名称
            val symbolName = getSymbolName(element)
            if (symbolName.isBlank()) {
                logger.warn("无法获取元素名称")
                return@withContext Context(elements = emptyList(), query = "")
            }

            // 获取符号上下文
            return@withContext contextEngine.getSymbolContext(symbolName, maxResults, minScore)
        } catch (e: Exception) {
            logger.error("从元素构建上下文时出错: ${element.text}", e)
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 从查询构建上下文
     *
     * @param query 查询文本
     * @param maxResults 最大结果数量
     * @param minScore 最小相似度分数
     * @param includeRelated 是否包含相关元素
     * @return 上下文
     */
    suspend fun buildFromQuery(
        query: String,
        maxResults: Int = 10,
        minScore: Double = 0.0,
        includeRelated: Boolean = true
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info("从查询构建上下文: $query")

            // 获取查询上下文
            return@withContext contextEngine.getQueryContext(query, maxResults, minScore, includeRelated)
        } catch (e: Exception) {
            logger.error("从查询构建上下文时出错: $query", e)
            return@withContext Context(elements = emptyList(), query = query)
        }
    }

    /**
     * 合并上下文
     *
     * @param contexts 上下文列表
     * @param maxElements 最大元素数量
     * @return 合并后的上下文
     */
    fun mergeContexts(contexts: List<Context>, maxElements: Int = 20): Context {
        // 如果上下文列表为空，则返回空上下文
        if (contexts.isEmpty()) {
            return Context(elements = emptyList(), query = "")
        }

        // 如果只有一个上下文，则直接返回
        if (contexts.size == 1) {
            return contexts.first()
        }

        // 合并所有元素
        val allElements = contexts.flatMap { it.elements }

        // 去重并按分数排序
        val uniqueElements = allElements
            .distinctBy { it.element.id }
            .sortedByDescending { it.relevance }
            .take(maxElements)

        // 合并查询
        val query = contexts.joinToString(" ") { it.query }

        // 创建合并后的上下文
        return Context(
            elements = uniqueElements,
            query = query
        )
    }

    /**
     * 从偏移量获取位置
     *
     * @param editor 编辑器
     * @param offset 偏移量
     * @return 位置
     */
    private fun getLocationFromOffset(editor: Editor, offset: Int): Location {
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val column = offset - lineStartOffset

        return Location(
            line = lineNumber + 1, // 转换为1-based
            column = column + 1, // 转换为1-based
            endLine = lineNumber + 1,
            endColumn = column + 1
        )
    }

    /**
     * 获取文件路径
     *
     * @param file 文件
     * @return 文件路径
     */
    private fun getFilePath(file: PsiFile): Path {
        val virtualFile = file.virtualFile
        return Paths.get(virtualFile.path)
    }

    /**
     * 获取符号名称
     *
     * @param element 元素
     * @return 符号名称
     */
    private fun getSymbolName(element: PsiElement): String {
        // 简单实现，实际应该根据不同语言的PSI结构获取符号名称
        return element.text.trim()
    }
}
