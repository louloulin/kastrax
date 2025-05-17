package ai.kastrax.code.ui.components

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import javax.swing.JFrame
import java.awt.BorderLayout
import java.awt.Dimension
import java.time.Instant

/**
 * 增强聊天面板测试
 */
class EnhancedChatPanelTest {

    private lateinit var project: Project
    private lateinit var conversation: ChatConversation
    private lateinit var codeAgentService: CodeAgentService
    private lateinit var conversationService: ConversationService

    @Before
    fun setUp() {
        // 模拟静态方法
        mockkStatic(CodeAgentService::class)
        mockkStatic(ConversationService::class)

        // 创建真实的 DeepSeek LLM
        val realAgent = agent {
            name = "DeepSeek测试代理"
            instructions = "你是一个专业的编程助手，擅长代码生成、解释、重构和测试。"
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CODER)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "sk-85e83081df28490b9ae63188f0cb4f79")
                temperature(0.3)
                maxTokens(2000)
            }
        }

        // 创建模拟对象
        project = mockk(relaxed = true)

        // 创建真实的 CodeAgentService 和 ConversationService
        // 注意：在实际测试中，我们仍然需要使用模拟对象，因为这些服务需要在 IntelliJ IDEA 环境中运行
        codeAgentService = mockk(relaxed = true)
        conversationService = mockk(relaxed = true)

        // 创建会话
        conversation = ChatConversation(
            id = "test-conversation",
            title = "Test Conversation",
            messages = mutableListOf(
                ChatMessage(
                    id = "1",
                    role = MessageRole.ASSISTANT,
                    content = "欢迎使用 KastraX Code！我是你的 AI 编程助手，可以帮助你编写、解释和重构代码。请在下方输入框中输入你的问题或指令。",
                    timestamp = Instant.now()
                )
            )
        )

        // 设置模拟行为
        every { CodeAgentService.getInstance(any()) } returns codeAgentService
        every { ConversationService.getInstance(any()) } returns conversationService
        every { conversationService.getCurrentConversation() } returns conversation
    }

    /**
     * 测试创建增强聊天面板
     */
    @Test
    fun testCreateEnhancedChatPanel() {
        // 创建增强聊天面板
        val chatPanel = EnhancedChatPanel(project, conversation)

        // 验证面板不为空
        assert(chatPanel != null)
    }

    /**
     * 测试显示会话消息
     */
    @Test
    fun testDisplayConversationMessages() {
        // 添加测试消息
        conversation.addMessage(
            ChatMessage(
                id = "2",
                role = MessageRole.USER,
                content = "请帮我写一个简单的 Kotlin 函数，计算斐波那契数列。",
                timestamp = Instant.now()
            )
        )

        conversation.addMessage(
            ChatMessage(
                id = "3",
                role = MessageRole.ASSISTANT,
                content = """
                    好的，下面是一个计算斐波那契数列的 Kotlin 函数：

                    ```kotlin
                    /**
                     * 计算斐波那契数列的第 n 个数
                     *
                     * @param n 位置（从0开始）
                     * @return 斐波那契数
                     */
                    fun fibonacci(n: Int): Int {
                        if (n <= 0) return 0
                        if (n == 1) return 1

                        var a = 0
                        var b = 1
                        var result = 0

                        for (i in 2..n) {
                            result = a + b
                            a = b
                            b = result
                        }

                        return result
                    }
                    ```

                    这个函数使用迭代方法计算斐波那契数列，比递归方法更高效。你可以这样使用它：

                    ```kotlin
                    fun main() {
                        // 打印斐波那契数列的前10个数
                        for (i in 0..9) {
                            println("fibonacci(i) = i")
                        }
                    }
                    ```

                    输出结果将是：

                    ```
                    fibonacci(0) = 0
                    fibonacci(1) = 1
                    fibonacci(2) = 1
                    fibonacci(3) = 2
                    fibonacci(4) = 3
                    fibonacci(5) = 5
                    fibonacci(6) = 8
                    fibonacci(7) = 13
                    fibonacci(8) = 21
                    fibonacci(9) = 34
                    ```
                """.trimIndent(),
                timestamp = Instant.now()
            )
        )

        // 创建增强聊天面板
        val chatPanel = EnhancedChatPanel(project, conversation)

        // 验证面板不为空
        assert(chatPanel != null)
    }

    /**
     * 手动测试UI（仅用于开发时手动运行）
     */
    fun manualTestUI() {
        // 添加测试消息
        conversation.addMessage(
            ChatMessage(
                id = "2",
                role = MessageRole.USER,
                content = "请帮我写一个简单的 Kotlin 函数，计算斐波那契数列。",
                timestamp = Instant.now()
            )
        )

        conversation.addMessage(
            ChatMessage(
                id = "3",
                role = MessageRole.ASSISTANT,
                content = """
                    好的，下面是一个计算斐波那契数列的 Kotlin 函数：

                    ```kotlin
                    /**
                     * 计算斐波那契数列的第 n 个数
                     *
                     * @param n 位置（从0开始）
                     * @return 斐波那契数
                     */
                    fun fibonacci(n: Int): Int {
                        if (n <= 0) return 0
                        if (n == 1) return 1

                        var a = 0
                        var b = 1
                        var result = 0

                        for (i in 2..n) {
                            result = a + b
                            a = b
                            b = result
                        }

                        return result
                    }
                    ```

                    这个函数使用迭代方法计算斐波那契数列，比递归方法更高效。你可以这样使用它：

                    ```kotlin
                    fun main() {
                        // 打印斐波那契数列的前10个数
                        for (i in 0..9) {
                            println("fibonacci(i) = i")
                        }
                    }
                    ```

                    输出结果将是：

                    ```
                    fibonacci(0) = 0
                    fibonacci(1) = 1
                    fibonacci(2) = 1
                    fibonacci(3) = 2
                    fibonacci(4) = 3
                    fibonacci(5) = 5
                    fibonacci(6) = 8
                    fibonacci(7) = 13
                    fibonacci(8) = 21
                    fibonacci(9) = 34
                    ```
                """.trimIndent(),
                timestamp = Instant.now()
            )
        )

        // 创建增强聊天面板
        val chatPanel = EnhancedChatPanel(project, conversation)

        // 创建窗口
        val frame = JFrame("增强聊天面板测试")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = BorderLayout()
        frame.add(chatPanel, BorderLayout.CENTER)
        frame.size = Dimension(1200, 800)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }
}
