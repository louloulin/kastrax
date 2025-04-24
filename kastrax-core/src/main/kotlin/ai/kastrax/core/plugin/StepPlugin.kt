package ai.kastrax.core.plugin

import ai.kastrax.core.workflow.WorkflowStep

/**
 * 工作流步骤插件接口，用于提供自定义工作流步骤。
 */
interface StepPlugin : Plugin {
    /**
     * 获取此插件提供的步骤类型列表。
     *
     * @return 步骤类型列表
     */
    fun getStepTypes(): List<StepType>
    
    /**
     * 创建步骤实例。
     *
     * @param stepType 步骤类型
     * @param config 步骤配置
     * @return 步骤实例
     */
    fun createStep(stepType: String, config: Map<String, Any?>): WorkflowStep?
}

/**
 * 步骤类型，描述了一种工作流步骤的类型。
 */
data class StepType(
    /**
     * 步骤类型的唯一标识符。
     */
    val id: String,
    
    /**
     * 步骤类型的名称。
     */
    val name: String,
    
    /**
     * 步骤类型的描述。
     */
    val description: String,
    
    /**
     * 步骤类型的图标（可选）。
     */
    val icon: String? = null,
    
    /**
     * 步骤类型的配置模式，描述了步骤配置的结构。
     */
    val configSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 步骤类型的输入模式，描述了步骤输入的结构。
     */
    val inputSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 步骤类型的输出模式，描述了步骤输出的结构。
     */
    val outputSchema: Map<String, ConfigField> = emptyMap(),
    
    /**
     * 步骤类型的分类。
     */
    val category: String = "其他",
    
    /**
     * 步骤类型的标签。
     */
    val tags: List<String> = emptyList()
)

/**
 * 配置字段，描述了配置中的一个字段。
 */
data class ConfigField(
    /**
     * 字段的名称。
     */
    val name: String,
    
    /**
     * 字段的类型。
     */
    val type: ConfigFieldType,
    
    /**
     * 字段的描述。
     */
    val description: String,
    
    /**
     * 字段是否必需。
     */
    val required: Boolean = false,
    
    /**
     * 字段的默认值。
     */
    val defaultValue: Any? = null,
    
    /**
     * 字段的可选值（用于枚举类型）。
     */
    val options: List<ConfigOption>? = null,
    
    /**
     * 字段的验证规则。
     */
    val validation: ConfigValidation? = null
)

/**
 * 配置字段类型枚举。
 */
enum class ConfigFieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    OBJECT,
    ARRAY,
    ENUM,
    DATE,
    TIME,
    DATETIME,
    FILE,
    CODE,
    EXPRESSION,
    REFERENCE
}

/**
 * 配置选项，用于枚举类型的字段。
 */
data class ConfigOption(
    /**
     * 选项的值。
     */
    val value: String,
    
    /**
     * 选项的标签。
     */
    val label: String,
    
    /**
     * 选项的描述。
     */
    val description: String? = null,
    
    /**
     * 选项的图标（可选）。
     */
    val icon: String? = null
)

/**
 * 配置验证，用于验证字段值。
 */
data class ConfigValidation(
    /**
     * 最小值（用于数字类型）。
     */
    val min: Number? = null,
    
    /**
     * 最大值（用于数字类型）。
     */
    val max: Number? = null,
    
    /**
     * 最小长度（用于字符串类型）。
     */
    val minLength: Int? = null,
    
    /**
     * 最大长度（用于字符串类型）。
     */
    val maxLength: Int? = null,
    
    /**
     * 正则表达式模式（用于字符串类型）。
     */
    val pattern: String? = null,
    
    /**
     * 自定义验证函数。
     */
    val validator: ((Any?) -> Boolean)? = null
)

/**
 * 抽象步骤插件基类，提供了StepPlugin接口的基本实现。
 */
abstract class AbstractStepPlugin(
    id: String,
    name: String,
    description: String,
    version: String,
    author: String,
    config: PluginConfig = DefaultPluginConfig()
) : AbstractPlugin(
    id = id,
    name = name,
    description = description,
    version = version,
    author = author,
    type = PluginType.WORKFLOW_STEP,
    config = config
), StepPlugin
