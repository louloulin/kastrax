package ai.kastrax.code.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 代码设置配置器
 * 
 * 提供 KastraX Code 的设置界面
 */
class CodeSettingsConfigurable : Configurable {
    
    private val settings = CodeSettingsState.getInstance()
    
    // UI 组件
    private var modelComboBox: ComboBox<String>? = null
    private var apiKeyField: JBTextField? = null
    private var maxTokensField: JBTextField? = null
    private var temperatureField: JBTextField? = null
    private var enableCompletionCheckBox: JBCheckBox? = null
    private var enableContextCheckBox: JBCheckBox? = null
    
    /**
     * 获取显示名称
     */
    override fun getDisplayName(): String = "KastraX Code"
    
    /**
     * 创建设置面板
     */
    override fun createComponent(): JComponent {
        // 创建 UI 组件
        modelComboBox = ComboBox(arrayOf("deepseek-coder", "gpt-4", "gpt-3.5-turbo", "claude-3-opus", "claude-3-sonnet"))
        apiKeyField = JBTextField(settings.apiKey)
        maxTokensField = JBTextField(settings.maxTokens.toString())
        temperatureField = JBTextField(settings.temperature.toString())
        enableCompletionCheckBox = JBCheckBox("启用代码补全", settings.enableCompletion)
        enableContextCheckBox = JBCheckBox("启用上下文感知", settings.enableContext)
        
        // 设置初始值
        modelComboBox?.selectedItem = settings.model
        
        // 创建面板
        val mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("模型:", modelComboBox!!)
            .addLabeledComponent("API 密钥:", apiKeyField!!)
            .addLabeledComponent("最大令牌数:", maxTokensField!!)
            .addLabeledComponent("温度:", temperatureField!!)
            .addComponent(enableCompletionCheckBox!!)
            .addComponent(enableContextCheckBox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        mainPanel.border = JBUI.Borders.empty(10)
        
        return mainPanel
    }
    
    /**
     * 检查设置是否已修改
     */
    override fun isModified(): Boolean {
        return modelComboBox?.selectedItem != settings.model ||
               apiKeyField?.text != settings.apiKey ||
               maxTokensField?.text?.toIntOrNull() != settings.maxTokens ||
               temperatureField?.text?.toDoubleOrNull() != settings.temperature ||
               enableCompletionCheckBox?.isSelected != settings.enableCompletion ||
               enableContextCheckBox?.isSelected != settings.enableContext
    }
    
    /**
     * 应用设置
     */
    override fun apply() {
        settings.model = modelComboBox?.selectedItem as? String ?: "deepseek-coder"
        settings.apiKey = apiKeyField?.text ?: ""
        settings.maxTokens = maxTokensField?.text?.toIntOrNull() ?: 2000
        settings.temperature = temperatureField?.text?.toDoubleOrNull() ?: 0.3
        settings.enableCompletion = enableCompletionCheckBox?.isSelected ?: true
        settings.enableContext = enableContextCheckBox?.isSelected ?: true
    }
    
    /**
     * 重置设置
     */
    override fun reset() {
        modelComboBox?.selectedItem = settings.model
        apiKeyField?.text = settings.apiKey
        maxTokensField?.text = settings.maxTokens.toString()
        temperatureField?.text = settings.temperature.toString()
        enableCompletionCheckBox?.isSelected = settings.enableCompletion
        enableContextCheckBox?.isSelected = settings.enableContext
    }
}
