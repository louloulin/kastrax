package ai.kastrax.core.workflow.registry

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.plugin.ConfigField
import ai.kastrax.core.plugin.StepPlugin
import ai.kastrax.core.plugin.StepType
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.io.StepProvider
import java.util.concurrent.ConcurrentHashMap

/**
 * 步骤注册表，用于管理自定义步骤类型。
 */
class StepRegistry : KastraXBase(component = "STEP_REGISTRY", name = "StepRegistry"), StepProvider {
    private val stepTypes = ConcurrentHashMap<String, StepType>()
    private val stepPlugins = ConcurrentHashMap<String, StepPlugin>()
    
    /**
     * 注册步骤类型。
     *
     * @param stepType 步骤类型
     * @param plugin 提供此步骤类型的插件
     * @return 注册是否成功
     */
    fun registerStepType(stepType: StepType, plugin: StepPlugin): Boolean {
        if (stepTypes.containsKey(stepType.id)) {
            logger.warn { "步骤类型已存在: ${stepType.id}" }
            return false
        }
        
        stepTypes[stepType.id] = stepType
        stepPlugins[stepType.id] = plugin
        
        logger.info { "注册步骤类型: ${stepType.name} (${stepType.id})" }
        return true
    }
    
    /**
     * 注销步骤类型。
     *
     * @param stepTypeId 步骤类型ID
     * @return 注销是否成功
     */
    fun unregisterStepType(stepTypeId: String): Boolean {
        val stepType = stepTypes.remove(stepTypeId)
        stepPlugins.remove(stepTypeId)
        
        if (stepType != null) {
            logger.info { "注销步骤类型: ${stepType.name} (${stepType.id})" }
            return true
        }
        
        return false
    }
    
    /**
     * 获取所有步骤类型。
     *
     * @return 所有步骤类型
     */
    fun getAllStepTypes(): List<StepType> {
        return stepTypes.values.toList()
    }
    
    /**
     * 获取步骤类型。
     *
     * @param stepTypeId 步骤类型ID
     * @return 步骤类型，如果不存在则返回null
     */
    fun getStepType(stepTypeId: String): StepType? {
        return stepTypes[stepTypeId]
    }
    
    /**
     * 获取指定分类的步骤类型。
     *
     * @param category 分类
     * @return 指定分类的步骤类型
     */
    fun getStepTypesByCategory(category: String): List<StepType> {
        return stepTypes.values.filter { it.category == category }
    }
    
    /**
     * 获取具有指定标签的步骤类型。
     *
     * @param tag 标签
     * @return 具有指定标签的步骤类型
     */
    fun getStepTypesByTag(tag: String): List<StepType> {
        return stepTypes.values.filter { it.tags.contains(tag) }
    }
    
    /**
     * 创建步骤实例。
     *
     * @param stepTypeId 步骤类型ID
     * @param id 步骤ID
     * @param name 步骤名称
     * @param description 步骤描述
     * @param after 前置步骤ID列表
     * @param variables 变量引用映射
     * @param config 步骤配置
     * @return 步骤实例，如果步骤类型不存在则返回null
     */
    fun createStep(
        stepTypeId: String,
        id: String,
        name: String,
        description: String,
        after: List<String>,
        variables: Map<String, VariableReference>,
        config: Map<String, Any?>
    ): WorkflowStep? {
        val plugin = stepPlugins[stepTypeId] ?: return null
        return plugin.createStep(stepTypeId, config)?.apply {
            // 使用反射设置步骤属性
            val idField = this.javaClass.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
            
            val nameField = this.javaClass.getDeclaredField("name")
            nameField.isAccessible = true
            nameField.set(this, name)
            
            val descriptionField = this.javaClass.getDeclaredField("description")
            descriptionField.isAccessible = true
            descriptionField.set(this, description)
            
            val afterField = this.javaClass.getDeclaredField("after")
            afterField.isAccessible = true
            afterField.set(this, after)
            
            val variablesField = this.javaClass.getDeclaredField("variables")
            variablesField.isAccessible = true
            variablesField.set(this, variables)
        }
    }
    
    /**
     * 验证步骤配置。
     *
     * @param stepTypeId 步骤类型ID
     * @param config 步骤配置
     * @return 验证结果
     */
    fun validateStepConfig(stepTypeId: String, config: Map<String, Any?>): ValidationResult {
        val stepType = stepTypes[stepTypeId] ?: return ValidationResult(false, "步骤类型不存在: $stepTypeId")
        
        val errors = mutableListOf<String>()
        
        // 验证必需字段
        for ((fieldName, field) in stepType.configSchema) {
            if (field.required && !config.containsKey(fieldName)) {
                errors.add("缺少必需字段: $fieldName")
            }
        }
        
        // 验证字段类型和值
        for ((fieldName, value) in config) {
            val field = stepType.configSchema[fieldName]
            if (field == null) {
                errors.add("未知字段: $fieldName")
                continue
            }
            
            if (value != null && !validateFieldType(field, value)) {
                errors.add("字段类型无效: $fieldName, 期望类型: ${field.type}, 实际类型: ${value::class.simpleName}")
            }
            
            if (value != null && field.validation != null && !validateFieldValue(field, value)) {
                errors.add("字段值无效: $fieldName, 值: $value")
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true)
        } else {
            ValidationResult(false, errors.joinToString("; "))
        }
    }
    
    /**
     * 验证字段类型。
     *
     * @param field 字段
     * @param value 值
     * @return 类型是否有效
     */
    private fun validateFieldType(field: ConfigField, value: Any): Boolean {
        return when (field.type) {
            ai.kastrax.core.plugin.ConfigFieldType.STRING -> value is String
            ai.kastrax.core.plugin.ConfigFieldType.NUMBER -> value is Number
            ai.kastrax.core.plugin.ConfigFieldType.BOOLEAN -> value is Boolean
            ai.kastrax.core.plugin.ConfigFieldType.OBJECT -> value is Map<*, *>
            ai.kastrax.core.plugin.ConfigFieldType.ARRAY -> value is List<*> || value is Array<*>
            ai.kastrax.core.plugin.ConfigFieldType.ENUM -> {
                if (value !is String) return false
                field.options?.any { it.value == value } ?: false
            }
            ai.kastrax.core.plugin.ConfigFieldType.DATE -> value is String && value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
            ai.kastrax.core.plugin.ConfigFieldType.TIME -> value is String && value.matches(Regex("\\d{2}:\\d{2}(:\\d{2})?"))
            ai.kastrax.core.plugin.ConfigFieldType.DATETIME -> value is String && value.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?(Z|[+-]\\d{2}:\\d{2})?"))
            ai.kastrax.core.plugin.ConfigFieldType.FILE -> value is String
            ai.kastrax.core.plugin.ConfigFieldType.CODE -> value is String
            ai.kastrax.core.plugin.ConfigFieldType.EXPRESSION -> value is String
            ai.kastrax.core.plugin.ConfigFieldType.REFERENCE -> value is String
        }
    }
    
    /**
     * 验证字段值。
     *
     * @param field 字段
     * @param value 值
     * @return 值是否有效
     */
    private fun validateFieldValue(field: ConfigField, value: Any): Boolean {
        val validation = field.validation ?: return true
        
        return when (field.type) {
            ai.kastrax.core.plugin.ConfigFieldType.STRING -> {
                if (value !is String) return false
                
                var valid = true
                if (validation.minLength != null && value.length < validation.minLength) {
                    valid = false
                }
                if (validation.maxLength != null && value.length > validation.maxLength) {
                    valid = false
                }
                if (validation.pattern != null && !value.matches(Regex(validation.pattern))) {
                    valid = false
                }
                
                valid
            }
            ai.kastrax.core.plugin.ConfigFieldType.NUMBER -> {
                if (value !is Number) return false
                
                var valid = true
                if (validation.min != null && value.toDouble() < validation.min.toDouble()) {
                    valid = false
                }
                if (validation.max != null && value.toDouble() > validation.max.toDouble()) {
                    valid = false
                }
                
                valid
            }
            else -> true
        }
    }
    
    /**
     * 实现StepProvider接口，用于创建步骤。
     */
    override fun createStep(
        id: String,
        name: String,
        description: String,
        after: List<String>,
        variables: Map<String, VariableReference>,
        config: StepConfig?
    ): WorkflowStep {
        // 从配置中获取步骤类型
        val stepTypeId = config?.getProperty("stepType") as? String
            ?: throw IllegalArgumentException("步骤配置中缺少stepType字段")
        
        // 从配置中获取步骤配置
        val stepConfig = config.getAllProperties().filterKeys { it != "stepType" }
        
        // 创建步骤
        return createStep(stepTypeId, id, name, description, after, variables, stepConfig)
            ?: throw IllegalArgumentException("无法创建步骤: $stepTypeId")
    }
}

/**
 * 验证结果。
 */
data class ValidationResult(
    val valid: Boolean,
    val message: String? = null
)
