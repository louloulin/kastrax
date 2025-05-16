package ai.kastrax.code.context

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 代码上下文提供者
 *
 * 提供不同类型的上下文
 */
@Service(Service.Level.PROJECT)
class CodeContextProvider(
    private val project: Project
) : KastraXCodeBase(component = "CODE_CONTEXT_PROVIDER") {

    // 使用父类的logger

    // 代码上下文引擎
    private val contextEngine by lazy { CodeContextEngineImpl.getInstance(project) }

    // 代码上下文构建器
    private val contextBuilder by lazy { CodeContextBuilder(project, contextEngine) }

    /**
     * 获取编辑器上下文
     *
     * @param editor 编辑器
     * @param contextTypes 上下文类型集合
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getEditorContext(
        editor: Editor,
        contextTypes: Set<ContextType> = setOf(ContextType.CURRENT_FILE, ContextType.RELATED_FILES),
        maxTokens: Int = 2000
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取编辑器上下文，类型: $contextTypes" }

            val contexts = mutableListOf<Context>()

            // 根据上下文类型获取不同的上下文
            if (ContextType.CURRENT_FILE in contextTypes) {
                // 获取当前文件上下文
                val editorContext = contextBuilder.buildFromEditor(editor)
                contexts.add(editorContext)
            }

            if (ContextType.RELATED_FILES in contextTypes) {
                // 获取相关文件上下文
                val editorContext = contextBuilder.buildFromEditor(editor, includeRelated = true)
                contexts.add(editorContext)
            }

            // 合并上下文
            return@withContext contextBuilder.mergeContexts(contexts)
        } catch (e: Exception) {
            logger.error(e) { "获取编辑器上下文时出错" }
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 获取文件上下文
     *
     * @param file 文件
     * @param contextTypes 上下文类型集合
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getFileContext(
        file: PsiFile,
        contextTypes: Set<ContextType> = setOf(ContextType.CURRENT_FILE, ContextType.RELATED_FILES),
        maxTokens: Int = 2000
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取文件上下文，类型: $contextTypes" }

            val contexts = mutableListOf<Context>()

            // 根据上下文类型获取不同的上下文
            if (ContextType.CURRENT_FILE in contextTypes) {
                // 获取当前文件上下文
                val fileContext = contextBuilder.buildFromFile(file)
                contexts.add(fileContext)
            }

            // 合并上下文
            return@withContext contextBuilder.mergeContexts(contexts)
        } catch (e: Exception) {
            logger.error(e) { "获取文件上下文时出错" }
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 获取元素上下文
     *
     * @param element 元素
     * @param contextTypes 上下文类型集合
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getElementContext(
        element: PsiElement,
        contextTypes: Set<ContextType> = setOf(ContextType.SYMBOL, ContextType.RELATED_SYMBOLS),
        maxTokens: Int = 2000
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取元素上下文，类型: $contextTypes" }

            val contexts = mutableListOf<Context>()

            // 根据上下文类型获取不同的上下文
            if (ContextType.SYMBOL in contextTypes) {
                // 获取符号上下文
                val elementContext = contextBuilder.buildFromElement(element)
                contexts.add(elementContext)
            }

            // 合并上下文
            return@withContext contextBuilder.mergeContexts(contexts)
        } catch (e: Exception) {
            logger.error(e) { "获取元素上下文时出错" }
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 获取查询上下文
     *
     * @param query 查询文本
     * @param contextTypes 上下文类型集合
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getQueryContext(
        query: String,
        contextTypes: Set<ContextType> = setOf(ContextType.SEMANTIC_SEARCH, ContextType.KEYWORD_SEARCH),
        maxTokens: Int = 2000
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取查询上下文，类型: $contextTypes" }

            // 获取查询上下文
            return@withContext contextBuilder.buildFromQuery(query)
        } catch (e: Exception) {
            logger.error(e) { "获取查询上下文时出错" }
            return@withContext Context(elements = emptyList(), query = query)
        }
    }

    /**
     * 获取项目概览上下文
     *
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getProjectOverviewContext(maxTokens: Int = 2000): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取项目概览上下文" }

            // 创建项目概览查询
            val query = "project overview"

            // 获取查询上下文
            return@withContext contextBuilder.buildFromQuery(query)
        } catch (e: Exception) {
            logger.error(e) { "获取项目概览上下文时出错" }
            return@withContext Context(elements = emptyList(), query = "project overview")
        }
    }

    companion object {
        /**
         * 获取项目的代码上下文提供者实例
         *
         * @param project 项目
         * @return 代码上下文提供者实例
         */
        fun getInstance(project: Project): CodeContextProvider {
            return project.service<CodeContextProvider>()
        }
    }
}
