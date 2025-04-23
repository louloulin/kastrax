package ai.kastrax.core.workflow.template

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowBuilder
import ai.kastrax.core.workflow.WorkflowStep
import mu.KotlinLogging

/**
 * 工作流模板，支持参数化工作流定义。
 *
 * @property templateName 模板名称
 * @property description 模板描述
 * @property parameters 模板参数定义
 * @property workflowBuilder 工作流构建器函数
 */
class WorkflowTemplate(
    templateName: String,
    val description: String,
    val parameters: List<TemplateParameter<*>>,
    private val workflowBuilder: (Map<String, Any?>) -> Workflow
) : KastraXBase(component = "WORKFLOW_TEMPLATE", name = templateName) {

    // Use the logger from KastraXBase

    /**
     * 使用指定参数创建工作流实例。
     *
     * @param parameters 参数值映射
     * @return 创建的工作流
     * @throws IllegalArgumentException 如果参数无效
     */
    fun createWorkflow(parameters: Map<String, Any?>): Workflow {
        logger.info { "从模板创建工作流: $name, 参数数量: ${parameters.size}" }

        // 验证参数
        validateParameters(parameters)

        // 创建工作流
        return workflowBuilder(parameters)
    }

    /**
     * 验证参数。
     *
     * @param parameters 参数值映射
     * @throws IllegalArgumentException 如果参数无效
     */
    private fun validateParameters(parameters: Map<String, Any?>) {
        // 检查必需参数
        val requiredParams = this.parameters.filter { it.required }
        for (param in requiredParams) {
            if (!parameters.containsKey(param.name)) {
                throw IllegalArgumentException("缺少必需参数: ${param.name}")
            }
        }

        // 验证参数类型和值
        for ((name, value) in parameters) {
            val paramDef = this.parameters.find { it.name == name }
                ?: throw IllegalArgumentException("未知参数: $name")

            if (value != null && !paramDef.validateType(value)) {
                throw IllegalArgumentException("参数类型无效: $name, 期望类型: ${paramDef.type.simpleName}, 实际类型: ${value::class.simpleName}")
            }

            if (value != null && !paramDef.validateValue(value)) {
                throw IllegalArgumentException("参数值无效: $name, 值: $value")
            }
        }
    }
}

/**
 * 模板参数。
 *
 * @param T 参数类型
 * @property name 参数名称
 * @property description 参数描述
 * @property type 参数类型
 * @property required 是否必需
 * @property defaultValue 默认值
 * @property validator 值验证器
 */
class TemplateParameter<T : Any>(
    val name: String,
    val description: String,
    val type: Class<T>,
    val required: Boolean = true,
    val defaultValue: T? = null,
    val validator: ((T) -> Boolean)? = null
) {
    /**
     * 验证值类型。
     *
     * @param value 要验证的值
     * @return 类型是否有效
     */
    fun validateType(value: Any): Boolean {
        return type.isInstance(value)
    }

    /**
     * 验证值。
     *
     * @param value 要验证的值
     * @return 值是否有效
     */
    @Suppress("UNCHECKED_CAST")
    fun validateValue(value: Any): Boolean {
        if (!validateType(value)) {
            return false
        }

        return validator?.invoke(value as T) ?: true
    }
}

/**
 * 工作流模板构建器。
 */
class WorkflowTemplateBuilder {
    var name: String = ""
    var description: String = ""
    private val parameters = mutableListOf<TemplateParameter<*>>()
    private var workflowBuilderFn: ((Map<String, Any?>) -> Workflow)? = null

    /**
     * 添加参数。
     *
     * @param T 参数类型
     * @param name 参数名称
     * @param description 参数描述
     * @param required 是否必需
     * @param defaultValue 默认值
     * @param validator 值验证器
     */
    fun <T : Any> parameter(
        name: String,
        description: String,
        type: Class<T>,
        required: Boolean = true,
        defaultValue: T? = null,
        validator: ((T) -> Boolean)? = null
    ) {
        parameters.add(
            TemplateParameter(
                name = name,
                description = description,
                type = type,
                required = required,
                defaultValue = defaultValue,
                validator = validator
            )
        )
    }

    /**
     * 设置工作流构建器函数。
     *
     * @param builder 工作流构建器函数
     */
    fun workflowBuilder(builder: (Map<String, Any?>) -> Workflow) {
        workflowBuilderFn = builder
    }

    /**
     * 设置工作流构建器DSL。
     *
     * @param builder 工作流构建器DSL函数
     */
    fun workflowBuilderDsl(builder: (Map<String, Any?>) -> WorkflowBuilder.() -> Unit) {
        workflowBuilderFn = { params ->
            val workflowBuilder = WorkflowBuilder()
            workflowBuilder.name = name
            workflowBuilder.description = description
            workflowBuilder.apply(builder(params))
            workflowBuilder.build()
        }
    }

    /**
     * 构建工作流模板。
     *
     * @return 创建的工作流模板
     */
    fun build(): WorkflowTemplate {
        require(name.isNotEmpty()) { "Template name must not be empty" }
        requireNotNull(workflowBuilderFn) { "Workflow builder function must be set" }

        return WorkflowTemplate(
            templateName = name,
            description = description,
            parameters = parameters,
            workflowBuilder = workflowBuilderFn!!
        )
    }
}

/**
 * 创建工作流模板的DSL函数。
 *
 * @param init 模板构建器初始化函数
 * @return 创建的工作流模板
 */
fun workflowTemplate(init: WorkflowTemplateBuilder.() -> Unit): WorkflowTemplate {
    val builder = WorkflowTemplateBuilder()
    builder.init()
    return builder.build()
}
