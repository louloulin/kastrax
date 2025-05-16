package ai.kastrax.code.context

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.ContextType
import ai.kastrax.code.model.Location
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 编辑器上下文提供者
 *
 * 获取当前编辑文件和光标位置的上下文
 */
class EditorContextProvider(
    private val project: Project,
    private val contextProvider: CodeContextProvider
) : KastraXCodeBase(component = "EDITOR_CONTEXT_PROVIDER") {

    // 使用父类的logger

    /**
     * 获取当前编辑器上下文
     *
     * @param editor 编辑器
     * @param contextTypes 上下文类型集合
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getCurrentEditorContext(
        editor: Editor,
        contextTypes: Set<ContextType> = setOf(ContextType.CURRENT_FILE, ContextType.RELATED_FILES),
        maxTokens: Int = 2000
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取当前编辑器上下文，类型: $contextTypes" }

            // 获取编辑器上下文
            val editorContext = contextProvider.getEditorContext(editor, contextTypes, maxTokens)

            // 获取当前光标位置的元素
            val currentElement = getCurrentElement(editor)

            // 如果有当前元素，则获取元素上下文
            val elementContext = if (currentElement != null) {
                contextProvider.getElementContext(
                    currentElement,
                    setOf(ContextType.SYMBOL, ContextType.RELATED_SYMBOLS),
                    maxTokens
                )
            } else {
                null
            }

            // 如果有元素上下文，则合并
            return@withContext if (elementContext != null) {
                mergeContexts(editorContext, elementContext)
            } else {
                editorContext
            }
        } catch (e: Exception) {
            logger.error(e) { "获取当前编辑器上下文时出错" }
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 获取选中代码上下文
     *
     * @param editor 编辑器
     * @param maxTokens 最大令牌数
     * @return 上下文
     */
    suspend fun getSelectedCodeContext(
        editor: Editor,
        maxTokens: Int = 2000
    ): Context = withContext(Dispatchers.IO) {
        try {
            logger.info { "获取选中代码上下文" }

            // 获取选中的文本
            val selectedText = editor.selectionModel.selectedText
            if (selectedText.isNullOrBlank()) {
                logger.warn { "没有选中文本" }
                return@withContext Context(elements = emptyList(), query = "")
            }

            // 获取当前文件
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (psiFile == null) {
                logger.warn { "无法获取当前文件" }
                return@withContext Context(elements = emptyList(), query = "")
            }

            // 获取选中文本的位置
            val selectionStart = editor.selectionModel.selectionStart
            val selectionEnd = editor.selectionModel.selectionEnd
            val startLocation = getLocationFromOffset(editor, selectionStart)
            val endLocation = getLocationFromOffset(editor, selectionEnd)

            // 创建上下文元素
            val element = ContextElement(
                id = "selected_code",
                name = "Selected Code",
                type = "CODE_SNIPPET",
                content = selectedText,
                filePath = getFilePath(psiFile),
                location = Location(
                    line = startLocation.line,
                    column = startLocation.column,
                    endLine = endLocation.line,
                    endColumn = endLocation.column
                ),
                score = 1.0
            )

            // 创建上下文
            return@withContext Context(
                elements = listOf(element),
                query = "selected code"
            )
        } catch (e: Exception) {
            logger.error(e) { "获取选中代码上下文时出错" }
            return@withContext Context(elements = emptyList(), query = "")
        }
    }

    /**
     * 获取当前光标位置的元素
     *
     * @param editor 编辑器
     * @return 元素
     */
    private fun getCurrentElement(editor: Editor): PsiElement? {
        try {
            // 获取当前文件
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null

            // 获取当前光标位置
            val offset = editor.caretModel.offset

            // 获取当前位置的元素
            return psiFile.findElementAt(offset)
        } catch (e: Exception) {
            logger.error(e) { "获取当前光标位置的元素时出错" }
            return null
        }
    }

    /**
     * 获取当前光标位置的最近的有意义的元素
     *
     * @param editor 编辑器
     * @return 元素
     */
    private fun getNearestMeaningfulElement(editor: Editor): PsiElement? {
        try {
            // 获取当前元素
            val currentElement = getCurrentElement(editor) ?: return null

            // 获取父元素
            return PsiTreeUtil.getParentOfType(
                currentElement,
                PsiElement::class.java,
                false
            )
        } catch (e: Exception) {
            logger.error(e) { "获取当前光标位置的最近的有意义的元素时出错" }
            return null
        }
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
    private fun getFilePath(file: com.intellij.psi.PsiFile): Path {
        val virtualFile = file.virtualFile
        return Paths.get(virtualFile.path)
    }

    /**
     * 合并上下文
     *
     * @param context1 上下文1
     * @param context2 上下文2
     * @return 合并后的上下文
     */
    private fun mergeContexts(context1: Context, context2: Context): Context {
        // 合并所有元素
        val allElements = context1.elements + context2.elements

        // 去重并按分数排序
        val uniqueElements = allElements
            .distinctBy { it.id }
            .sortedByDescending { it.score }

        // 合并查询
        val query = "${context1.query} ${context2.query}".trim()

        // 创建合并后的上下文
        return Context(
            elements = uniqueElements,
            query = query
        )
    }
}
