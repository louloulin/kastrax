package ai.kastrax.code.workflow

import ai.kastrax.code.model.Context

/**
 * 工作流模板接口
 *
 * 定义工作流模板的基本操作
 */
interface WorkflowTemplate {
    /**
     * 模板ID
     */
    val id: String
    
    /**
     * 模板名称
     */
    val name: String
    
    /**
     * 模板描述
     */
    val description: String
    
    /**
     * 模板类别
     */
    val category: String
    
    /**
     * 模板参数
     */
    val parameters: List<TemplateParameter>
    
    /**
     * 创建工作流
     *
     * @param parameterValues 参数值
     * @return 创建的工作流
     */
    fun createWorkflow(parameterValues: Map<String, Any>): CodeWorkflow
    
    /**
     * 验证参数
     *
     * @param parameterValues 参数值
     * @return 验证结果
     */
    fun validateParameters(parameterValues: Map<String, Any>): ParameterValidationResult
}

/**
 * 模板参数
 *
 * @property name 参数名称
 * @property description 参数描述
 * @property type 参数类型
 * @property required 是否必需
 * @property defaultValue 默认值
 * @property options 选项列表
 */
data class TemplateParameter(
    val name: String,
    val description: String,
    val type: ParameterType,
    val required: Boolean = true,
    val defaultValue: Any? = null,
    val options: List<ParameterOption> = emptyList()
)

/**
 * 参数类型
 */
enum class ParameterType {
    /**
     * 字符串
     */
    STRING,
    
    /**
     * 数字
     */
    NUMBER,
    
    /**
     * 布尔值
     */
    BOOLEAN,
    
    /**
     * 选项
     */
    OPTION,
    
    /**
     * 文件路径
     */
    FILE_PATH,
    
    /**
     * 目录路径
     */
    DIRECTORY_PATH,
    
    /**
     * 代码片段
     */
    CODE_SNIPPET,
    
    /**
     * 上下文
     */
    CONTEXT
}

/**
 * 参数选项
 *
 * @property value 选项值
 * @property label 选项标签
 * @property description 选项描述
 */
data class ParameterOption(
    val value: String,
    val label: String,
    val description: String = ""
)

/**
 * 参数验证结果
 *
 * @property valid 是否有效
 * @property errors 错误列表
 */
data class ParameterValidationResult(
    val valid: Boolean,
    val errors: Map<String, String> = emptyMap()
)

/**
 * 工作流模板注册表接口
 *
 * 定义工作流模板注册表的基本操作
 */
interface WorkflowTemplateRegistry {
    /**
     * 注册模板
     *
     * @param template 模板
     * @return 是否成功注册
     */
    fun registerTemplate(template: WorkflowTemplate): Boolean
    
    /**
     * 注销模板
     *
     * @param templateId 模板ID
     * @return 是否成功注销
     */
    fun unregisterTemplate(templateId: String): Boolean
    
    /**
     * 获取模板
     *
     * @param templateId 模板ID
     * @return 模板
     */
    fun getTemplate(templateId: String): WorkflowTemplate?
    
    /**
     * 获取所有模板
     *
     * @return 模板列表
     */
    fun getAllTemplates(): List<WorkflowTemplate>
    
    /**
     * 获取分类模板
     *
     * @param category 分类
     * @return 模板列表
     */
    fun getTemplatesByCategory(category: String): List<WorkflowTemplate>
    
    /**
     * 搜索模板
     *
     * @param query 查询字符串
     * @return 模板列表
     */
    fun searchTemplates(query: String): List<WorkflowTemplate>
}
