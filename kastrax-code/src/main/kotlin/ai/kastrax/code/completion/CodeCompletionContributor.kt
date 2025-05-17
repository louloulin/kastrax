package ai.kastrax.code.completion

import ai.kastrax.code.service.CodeAgentService
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import kotlinx.coroutines.runBlocking
import javax.swing.Icon

/**
 * 代码补全贡献者
 *
 * 提供基于 AI 的代码补全
 */
class CodeCompletionContributor : CompletionContributor() {

    init {
        // 注册补全提供者
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    // 获取当前文件内容和光标位置
                    val editor = parameters.editor
                    val document = editor.document
                    val offset = parameters.offset
                    val prefix = result.prefixMatcher.prefix

                    // 如果前缀太短，不提供补全
                    if (prefix.length < 2) {
                        return
                    }

                    // 获取当前行
                    val lineNumber = document.getLineNumber(offset)
                    val lineStartOffset = document.getLineStartOffset(lineNumber)
                    val lineEndOffset = document.getLineEndOffset(lineNumber)
                    val currentLine = document.getText(TextRange(lineStartOffset, offset))

                    // 获取前几行作为上下文
                    val contextStartLine = maxOf(0, lineNumber - 5)
                    val contextStartOffset = document.getLineStartOffset(contextStartLine)
                    val context = document.getText(TextRange(contextStartOffset, offset))

                    // 调用代码智能体生成补全
                    val project = parameters.originalFile.project
                    val codeAgent = CodeAgentService.getInstance(project).getCodeAgent()

                    // 推断语言
                    val language = parameters.originalFile.language.displayName.lowercase()

                    // 运行在阻塞模式下，实际应用中应该使用异步方式
                    runBlocking {
                        try {
                            // 生成补全
                            val completion = codeAgent.complete(context, language, 100)

                            // 添加补全结果
                            if (completion.isNotEmpty()) {
                                // 分割成多行
                                val lines = completion.split("\n")

                                // 添加第一行作为直接补全
                                val firstLine = lines.firstOrNull()?.trim() ?: ""
                                if (firstLine.isNotEmpty()) {
                                    result.addElement(
                                        LookupElementBuilder.create(firstLine)
                                            .withPresentableText(firstLine)
                                            .withTypeText("KastraX", true)
                                            .withIcon(KastraxIcon)
                                    )
                                }

                                // 如果有多行，添加完整补全
                                if (lines.size > 1) {
                                    result.addElement(
                                        LookupElementBuilder.create(completion)
                                            .withPresentableText("完整补全...")
                                            .withTypeText("KastraX", true)
                                            .withIcon(KastraxIcon)
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略错误
                        }
                    }
                }
            }
        )
    }

    /**
     * KastraX 图标
     */
    private object KastraxIcon : Icon {
        override fun paintIcon(c: java.awt.Component, g: java.awt.Graphics, x: Int, y: Int) {
            // 简单的蓝色方块图标
            g.color = java.awt.Color(58, 134, 255)
            g.fillRect(x, y, iconWidth, iconHeight)
            g.color = java.awt.Color.WHITE
            g.drawLine(x + 3, y + 3, x + 7, y + 7)
            g.drawLine(x + 7, y + 7, x + 3, y + 11)
            g.drawLine(x + 11, y + 3, x + 7, y + 7)
            g.drawLine(x + 7, y + 7, x + 11, y + 11)
        }

        override fun getIconWidth(): Int = 14

        override fun getIconHeight(): Int = 14
    }
}
