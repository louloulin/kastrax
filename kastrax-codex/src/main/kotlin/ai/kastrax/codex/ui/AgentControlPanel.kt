package ai.kastrax.codex.ui

import ai.kastrax.codex.service.AgentConfig
import ai.kastrax.codex.service.AgentStatus
import ai.kastrax.codex.service.AgentType
import ai.kastrax.codex.service.CodexAgentService
import ai.kastrax.core.agent.Agent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * 智能体控制面板UI组件
 *
 * @property project 项目
 * @property agentService 智能体服务
 */
class AgentControlPanel(
    private val project: Project,
    private val agentService: CodexAgentService
) : JPanel(BorderLayout()) {

    private val logger = Logger.getInstance(AgentControlPanel::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.Swing)

    private val agentStatusLabel = JBLabel("智能体状态: ${AgentStatus.IDLE.name}")
    private val startAgentButton = JButton("启动智能体")
    private val stopAgentButton = JButton("停止智能体")
    private val agentTypeComboBox = JComboBox(AgentType.values())
    private val promptField = JTextField(20)
    private val sendButton = JButton("发送")
    private val responseArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
    }

    private var currentAgent: Agent? = null

    init {
        // 设置边距
        border = JBUI.Borders.empty(10)

        // 创建顶部控制面板
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JBLabel("智能体类型:"))
            add(agentTypeComboBox)
            add(agentStatusLabel)
            add(startAgentButton)
            add(stopAgentButton)
        }

        // 创建输入面板
        val inputPanel = JPanel(BorderLayout()).apply {
            add(JBLabel("提示:"), BorderLayout.WEST)
            add(promptField, BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
            border = JBUI.Borders.empty(5)
        }

        // 创建响应区域
        val responsePanel = JBScrollPane(responseArea).apply {
            border = JBUI.Borders.empty(5)
        }

        // 添加组件到面板
        add(controlPanel, BorderLayout.NORTH)
        add(inputPanel, BorderLayout.CENTER)
        add(responsePanel, BorderLayout.SOUTH)

        // 配置按钮状态
        stopAgentButton.isEnabled = false
        sendButton.isEnabled = false

        // 添加按钮事件监听器
        startAgentButton.addActionListener {
            startAgent()
        }

        stopAgentButton.addActionListener {
            stopAgent()
        }

        sendButton.addActionListener {
            sendPrompt()
        }
    }

    /**
     * 启动智能体
     */
    private fun startAgent() {
        val selectedType = agentTypeComboBox.selectedItem as AgentType
        
        // 更新UI状态
        startAgentButton.isEnabled = false
        agentStatusLabel.text = "智能体状态: 正在启动..."
        
        // 在协程中创建智能体
        coroutineScope.launch {
            try {
                // 创建智能体配置
                val config = AgentConfig(
                    name = "Codex${selectedType.name}Agent",
                    type = selectedType,
                    instructions = "你是一个编程助手，专注于帮助开发者编写、理解和改进代码。",
                    apiKey = "your-api-key-here" // 实际应用中应从设置中获取
                )
                
                // 创建智能体
                currentAgent = agentService.createProgrammingAgent(config)
                
                // 更新UI状态
                updateAgentStatus(AgentStatus.IDLE)
                sendButton.isEnabled = true
                responseArea.text = "智能体已启动，类型: ${selectedType.name}\n"
            } catch (e: Exception) {
                logger.error("启动智能体失败", e)
                responseArea.text = "启动智能体失败: ${e.message}\n"
                updateAgentStatus(AgentStatus.ERROR)
            }
        }
    }

    /**
     * 停止智能体
     */
    private fun stopAgent() {
        currentAgent?.let {
            agentService.terminateAgent(it)
            currentAgent = null
            updateAgentStatus(AgentStatus.STOPPED)
            responseArea.text += "智能体已停止\n"
        }
    }

    /**
     * 发送提示
     */
    private fun sendPrompt() {
        val prompt = promptField.text
        if (prompt.isBlank() || currentAgent == null) return
        
        // 更新UI状态
        sendButton.isEnabled = false
        updateAgentStatus(AgentStatus.BUSY)
        responseArea.text += "\n用户: $prompt\n"
        
        // 在协程中获取响应
        coroutineScope.launch {
            try {
                // 获取响应
                agentService.getResponse(currentAgent!!, prompt).collect { response ->
                    responseArea.text += "智能体: ${response.text}\n"
                }
                
                // 更新UI状态
                updateAgentStatus(AgentStatus.IDLE)
                sendButton.isEnabled = true
                promptField.text = ""
            } catch (e: Exception) {
                logger.error("获取响应失败", e)
                responseArea.text += "获取响应失败: ${e.message}\n"
                updateAgentStatus(AgentStatus.ERROR)
                sendButton.isEnabled = true
            }
        }
    }

    /**
     * 更新智能体状态
     */
    fun updateAgentStatus(status: AgentStatus) {
        agentStatusLabel.text = "智能体状态: ${status.name}"
        // 根据状态更新UI
        when (status) {
            AgentStatus.IDLE -> {
                startAgentButton.isEnabled = currentAgent == null
                stopAgentButton.isEnabled = currentAgent != null
                sendButton.isEnabled = currentAgent != null
            }
            AgentStatus.BUSY -> {
                startAgentButton.isEnabled = false
                stopAgentButton.isEnabled = true
                sendButton.isEnabled = false
            }
            AgentStatus.STOPPED -> {
                startAgentButton.isEnabled = true
                stopAgentButton.isEnabled = false
                sendButton.isEnabled = false
            }
            AgentStatus.ERROR -> {
                startAgentButton.isEnabled = true
                stopAgentButton.isEnabled = currentAgent != null
                sendButton.isEnabled = currentAgent != null
            }
        }
    }
}
