package ai.kastrax.core.workflow.io

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.StepConfig
import java.time.Duration
import kotlinx.serialization.json.*
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowStep
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.Contextual
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID

/**
 * 工作流导入/导出工具，用于工作流的序列化和反序列化。
 */
class WorkflowIO : KastraXBase(component = "WORKFLOW_IO", name = "WorkflowIO") {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 将工作流导出为JSON字符串。
     *
     * @param workflow 工作流
     * @return JSON字符串
     */
    fun exportToJson(workflow: Workflow): String {
        val workflowData = serializeWorkflow(workflow)
        return json.encodeToString(WorkflowData.serializer(), workflowData)
    }

    /**
     * 将工作流导出为JSON元素。
     *
     * @param workflow 工作流
     * @return JSON元素
     */
    fun exportToJsonElement(workflow: Workflow): JsonElement {
        val workflowData = serializeWorkflow(workflow)
        return json.encodeToJsonElement(workflowData)
    }

    /**
     * 将工作流导出到文件。
     *
     * @param workflow 工作流
     * @param file 文件
     * @param format 导出格式
     */
    fun exportToFile(workflow: Workflow, file: File, format: ExportFormat = ExportFormat.JSON) {
        val content = when (format) {
            ExportFormat.JSON -> exportToJson(workflow)
            ExportFormat.YAML -> exportToYaml(workflow)
        }

        FileWriter(file).use { writer ->
            writer.write(content)
        }

        logger.info { "工作流已导出到文件: ${file.absolutePath}" }
    }

    /**
     * 将工作流导出为YAML字符串。
     *
     * @param workflow 工作流
     * @return YAML字符串
     */
    fun exportToYaml(workflow: Workflow): String {
        // 使用SnakeYAML或其他YAML库实现
        // 这里简单地将JSON转换为YAML格式
        val jsonStr = exportToJson(workflow)
        return jsonToYaml(jsonStr)
    }

    /**
     * 从JSON字符串导入工作流。
     *
     * @param jsonStr JSON字符串
     * @param stepProviders 步骤提供者映射，用于创建自定义步骤
     * @return 工作流
     */
    fun importFromJson(jsonStr: String, stepProviders: Map<String, StepProvider> = emptyMap()): Workflow {
        val workflowData = json.decodeFromString(WorkflowData.serializer(), jsonStr)
        return deserializeWorkflow(workflowData, stepProviders)
    }

    /**
     * 从JSON元素导入工作流。
     *
     * @param jsonElement JSON元素
     * @param stepProviders 步骤提供者映射，用于创建自定义步骤
     * @return 工作流
     */
    fun importFromJsonElement(jsonElement: JsonElement, stepProviders: Map<String, StepProvider> = emptyMap()): Workflow {
        val workflowData = json.decodeFromJsonElement(WorkflowData.serializer(), jsonElement)
        return deserializeWorkflow(workflowData, stepProviders)
    }

    /**
     * 从文件导入工作流。
     *
     * @param file 文件
     * @param format 导入格式
     * @param stepProviders 步骤提供者映射，用于创建自定义步骤
     * @return 工作流
     */
    fun importFromFile(file: File, format: ExportFormat = ExportFormat.JSON, stepProviders: Map<String, StepProvider> = emptyMap()): Workflow {
        val content = FileReader(file).use { reader ->
            reader.readText()
        }

        return when (format) {
            ExportFormat.JSON -> importFromJson(content, stepProviders)
            ExportFormat.YAML -> importFromYaml(content, stepProviders)
        }
    }

    /**
     * 从YAML字符串导入工作流。
     *
     * @param yamlStr YAML字符串
     * @param stepProviders 步骤提供者映射，用于创建自定义步骤
     * @return 工作流
     */
    fun importFromYaml(yamlStr: String, stepProviders: Map<String, StepProvider> = emptyMap()): Workflow {
        // 使用SnakeYAML或其他YAML库实现
        // 这里简单地将YAML转换为JSON格式
        val jsonStr = yamlToJson(yamlStr)
        return importFromJson(jsonStr, stepProviders)
    }

    /**
     * 将工作流序列化为WorkflowData。
     *
     * @param workflow 工作流
     * @return WorkflowData
     */
    private fun serializeWorkflow(workflow: Workflow): WorkflowData {
        val steps = mutableListOf<StepData>()

        // 获取工作流步骤
        val workflowSteps = getWorkflowSteps(workflow)

        // 序列化每个步骤
        for (step in workflowSteps) {
            val stepData = StepData(
                id = step.id,
                name = step.name,
                description = step.description,
                type = step.javaClass.name,
                after = step.after,
                variables = serializeVariables(step.variables),
                config = serializeStepConfig(step.config),
                condition = null // 条件暂不支持序列化
            )
            steps.add(stepData)
        }

        return WorkflowData(
            id = UUID.randomUUID().toString(), // 生成新的ID
            name = workflow.javaClass.getField("name").get(workflow) as String,
            description = workflow.javaClass.getField("description").get(workflow) as String,
            steps = steps,
            metadata = mapOf(
                "exportTime" to System.currentTimeMillis().toString(),
                "exportVersion" to "1.0"
            )
        )
    }

    /**
     * 将WorkflowData反序列化为Workflow。
     *
     * @param workflowData WorkflowData
     * @param stepProviders 步骤提供者映射，用于创建自定义步骤
     * @return Workflow
     */
    private fun deserializeWorkflow(workflowData: WorkflowData, stepProviders: Map<String, StepProvider>): Workflow {
        val steps = mutableMapOf<String, WorkflowStep>()

        // 反序列化每个步骤
        for (stepData in workflowData.steps) {
            val step = deserializeStep(stepData, stepProviders)
            steps[step.id] = step
        }

        return SimpleWorkflow(
            workflowName = workflowData.name,
            description = workflowData.description,
            steps = steps
        )
    }

    /**
     * 将StepData反序列化为WorkflowStep。
     *
     * @param stepData StepData
     * @param stepProviders 步骤提供者映射，用于创建自定义步骤
     * @return WorkflowStep
     */
    private fun deserializeStep(stepData: StepData, stepProviders: Map<String, StepProvider>): WorkflowStep {
        // 尝试使用步骤提供者创建步骤
        val stepType = stepData.type
        val stepProvider = stepProviders[stepType]

        if (stepProvider != null) {
            // 使用步骤提供者创建步骤
            val config = deserializeStepConfig(stepData.config)
            val variables = deserializeVariables(stepData.variables)

            return stepProvider.createStep(
                id = stepData.id,
                name = stepData.name,
                description = stepData.description,
                after = stepData.after,
                variables = variables,
                config = config
            )
        } else {
            // 如果没有找到步骤提供者，创建一个通用步骤
            logger.warn { "未找到步骤提供者: $stepType，创建通用步骤" }

            return object : WorkflowStep {
                override val id: String = stepData.id
                override val name: String = stepData.name
                override val description: String = stepData.description
                override val after: List<String> = stepData.after
                override val variables: Map<String, VariableReference> = deserializeVariables(stepData.variables)
                override val config: StepConfig? = deserializeStepConfig(stepData.config)

                override suspend fun execute(context: ai.kastrax.core.workflow.WorkflowContext): ai.kastrax.core.workflow.WorkflowStepResult {
                    throw UnsupportedOperationException("通用步骤不支持执行")
                }
            }
        }
    }

    /**
     * 序列化变量引用映射。
     *
     * @param variables 变量引用映射
     * @return 序列化后的变量引用映射
     */
    private fun serializeVariables(variables: Map<String, VariableReference>): Map<String, VariableData> {
        return variables.mapValues { (_, variable) ->
            VariableData(
                source = variable.path,
                path = variable.path
            )
        }
    }

    /**
     * 反序列化变量引用映射。
     *
     * @param variables 序列化后的变量引用映射
     * @return 变量引用映射
     */
    private fun deserializeVariables(variables: Map<String, VariableData>): Map<String, VariableReference> {
        return variables.mapValues { (_, variable) ->
            VariableReference(
                path = variable.path
            )
        }
    }

    /**
     * 序列化步骤配置。
     *
     * @param config 步骤配置
     * @return 序列化后的步骤配置
     */
    private fun serializeStepConfig(config: StepConfig?): Map<String, JsonElement>? {
        if (config == null) return null

        val result = mutableMapOf<String, JsonElement>()

        // 使用反射获取配置属性
        val fields = config.javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            val value = field.get(config)
            if (value != null) {
                result[field.name] = when (value) {
                    is String -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is JsonElement -> value
                    else -> JsonPrimitive(value.toString())
                }
            }
        }

        return result
    }

    /**
     * 反序列化步骤配置。
     *
     * @param config 序列化后的步骤配置
     * @return 步骤配置
     */
    private fun deserializeStepConfig(config: Map<String, JsonElement>?): StepConfig? {
        if (config == null) return null

        val timeout = config["timeout"]?.let {
            when (val value = deserializeJsonElement(it)) {
                is Long -> Duration.ofMillis(value)
                is Int -> Duration.ofMillis(value.toLong())
                else -> Duration.ofMinutes(5)
            }
        } ?: Duration.ofMinutes(5)

        val maxTokens = config["maxTokens"]?.let {
            when (val value = deserializeJsonElement(it)) {
                is Int -> value
                is Long -> value.toInt()
                else -> null
            }
        }

        val tags = config["tags"]?.let {
            when (val value = deserializeJsonElement(it)) {
                is List<*> -> value.filterIsInstance<String>().toSet()
                else -> emptySet<String>()
            }
        } ?: emptySet()

        return StepConfig(
            timeout = timeout,
            maxTokens = maxTokens,
            tags = tags
        )
    }

    /**
     * 将JsonElement反序列化为Kotlin对象。
     *
     * @param element JSON元素
     * @return 反序列化后的对象
     */
    private fun deserializeJsonElement(element: JsonElement): Any {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.booleanOrNull != null -> element.boolean
                    element.intOrNull != null -> element.int
                    element.longOrNull != null -> element.long
                    element.doubleOrNull != null -> element.double
                    else -> element.toString()
                }
            }
            is JsonObject -> deserializeStepConfig(element.jsonObject.toMap()) ?: StepConfig()
            is JsonArray -> element.jsonArray.map { deserializeJsonElement(it) }
            else -> element.toString()
        }
    }

    /**
     * 获取工作流的所有步骤。
     *
     * @param workflow 工作流
     * @return 步骤列表
     */
    private fun getWorkflowSteps(workflow: Workflow): List<WorkflowStep> {
        // 使用反射获取步骤
        val stepsField = workflow.javaClass.getDeclaredField("steps")
        stepsField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val stepsMap = stepsField.get(workflow) as Map<String, WorkflowStep>
        return stepsMap.values.toList()
    }

    /**
     * 将JSON转换为YAML。
     *
     * @param jsonStr JSON字符串
     * @return YAML字符串
     */
    private fun jsonToYaml(jsonStr: String): String {
        // 使用SnakeYAML或其他YAML库实现
        // 这里简单地返回JSON字符串，实际实现需要转换
        return jsonStr
    }

    /**
     * 将YAML转换为JSON。
     *
     * @param yamlStr YAML字符串
     * @return JSON字符串
     */
    private fun yamlToJson(yamlStr: String): String {
        // 使用SnakeYAML或其他YAML库实现
        // 这里简单地返回YAML字符串，实际实现需要转换
        return yamlStr
    }
}

/**
 * 导出格式枚举。
 */
enum class ExportFormat {
    /**
     * JSON格式。
     */
    JSON,

    /**
     * YAML格式。
     */
    YAML
}

/**
 * 工作流数据，用于序列化和反序列化。
 */
@Serializable
data class WorkflowData(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<StepData>,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 步骤数据，用于序列化和反序列化。
 */
@Serializable
data class StepData(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val after: List<String>,
    @Contextual
    val variables: Map<String, VariableData>,
    @Contextual
    val config: Map<String, JsonElement>? = null,
    val condition: String? = null
)

/**
 * 变量数据，用于序列化和反序列化。
 */
@Serializable
data class VariableData(
    val source: String,
    val path: String
)

/**
 * 步骤提供者接口，用于创建自定义步骤。
 */
interface StepProvider {
    /**
     * 创建步骤。
     *
     * @param id 步骤ID
     * @param name 步骤名称
     * @param description 步骤描述
     * @param after 前置步骤ID列表
     * @param variables 变量引用映射
     * @param config 步骤配置
     * @return 步骤实例
     */
    fun createStep(
        id: String,
        name: String,
        description: String,
        after: List<String>,
        variables: Map<String, VariableReference>,
        config: StepConfig?
    ): WorkflowStep
}
