package ai.kastrax.examples

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.tools.Tool

/**
 * 测试代理定义。
 */
fun main() {
    // 定义工具
    val calculatorTool = tool("calculator") {
        description = "一个简单的计算器工具"
        
        execute { input ->
            val expression = input.get("expression").asString()
            val result = evaluateExpression(expression)
            mapOf("result" to result)
        }
    }
    
    val weatherTool = tool("weather") {
        description = "获取天气信息"
        
        execute { input ->
            val location = input.get("location").asString()
            val weather = getWeather(location)
            mapOf("weather" to weather)
        }
    }
    
    // 定义代理
    val testAgent = agent("test-agent") {
        description = "测试代理"
        
        tools(calculatorTool, weatherTool)
        
        systemPrompt("""
            你是一个测试代理，可以使用计算器和天气工具。
        """.trimIndent())
    }
    
    // 定义工作流
    val testWorkflow = workflow("test-workflow") {
        description = "测试工作流"
        
        input {
            field("query", "string", "用户查询")
        }
        
        step("agent-step") {
            agent = testAgent
            input = { context ->
                mapOf("query" to context.input["query"])
            }
        }
        
        output { context ->
            mapOf("result" to context.steps["agent-step"].output)
        }
    }
    
    println("测试代理和工作流已定义")
}

/**
 * 计算表达式。
 */
fun evaluateExpression(expression: String): Double {
    // 简单实现，仅支持加减乘除
    return 42.0
}

/**
 * 获取天气信息。
 */
fun getWeather(location: String): String {
    // 模拟实现
    return "晴天"
}

/**
 * 工具 DSL。
 */
fun tool(id: String, init: ToolBuilder.() -> Unit): Tool {
    val builder = ToolBuilder(id)
    builder.init()
    return builder.build()
}

/**
 * 工具构建器。
 */
class ToolBuilder(val id: String) {
    var description: String = ""
    private var executeBlock: (Map<String, Any>) -> Map<String, Any> = { emptyMap() }
    
    fun execute(block: (Map<String, Any>) -> Map<String, Any>) {
        executeBlock = block
    }
    
    fun build(): Tool {
        // 简化实现
        return object : Tool {
            override val id: String = this@ToolBuilder.id
            override val description: String = this@ToolBuilder.description
            
            override fun execute(input: Map<String, Any>): Map<String, Any> {
                return executeBlock(input)
            }
        }
    }
}

/**
 * 代理 DSL。
 */
fun agent(id: String, init: AgentBuilder.() -> Unit): Agent {
    val builder = AgentBuilder(id)
    builder.init()
    return builder.build()
}

/**
 * 代理构建器。
 */
class AgentBuilder(val id: String) {
    var description: String = ""
    private var systemPromptText: String = ""
    private val toolsList = mutableListOf<Tool>()
    
    fun systemPrompt(text: String) {
        systemPromptText = text
    }
    
    fun tools(vararg tools: Tool) {
        toolsList.addAll(tools)
    }
    
    fun build(): Agent {
        // 简化实现
        return object : Agent {
            override val id: String = this@AgentBuilder.id
            override val description: String = this@AgentBuilder.description
            
            override fun generate(input: String): String {
                return "这是代理 $id 的响应"
            }
        }
    }
}

/**
 * 工作流 DSL。
 */
fun workflow(id: String, init: WorkflowBuilder.() -> Unit): Workflow {
    val builder = WorkflowBuilder(id)
    builder.init()
    return builder.build()
}

/**
 * 工作流构建器。
 */
class WorkflowBuilder(val id: String) {
    var description: String = ""
    private val steps = mutableMapOf<String, WorkflowStep>()
    private var inputBlock: InputBuilder.() -> Unit = {}
    private var outputBlock: (WorkflowContext) -> Map<String, Any> = { emptyMap() }
    
    fun input(block: InputBuilder.() -> Unit) {
        inputBlock = block
    }
    
    fun step(id: String, init: StepBuilder.() -> Unit) {
        val builder = StepBuilder(id)
        builder.init()
        steps[id] = builder.build()
    }
    
    fun output(block: (WorkflowContext) -> Map<String, Any>) {
        outputBlock = block
    }
    
    fun build(): Workflow {
        // 简化实现
        return object : Workflow {
            override val id: String = this@WorkflowBuilder.id
            override val description: String = this@WorkflowBuilder.description
            
            override fun execute(input: Map<String, Any>): Map<String, Any> {
                return mapOf("result" to "这是工作流 $id 的结果")
            }
        }
    }
}

/**
 * 输入构建器。
 */
class InputBuilder {
    private val fields = mutableMapOf<String, InputField>()
    
    fun field(name: String, type: String, description: String) {
        fields[name] = InputField(name, type, description)
    }
}

/**
 * 输入字段。
 */
data class InputField(val name: String, val type: String, val description: String)

/**
 * 步骤构建器。
 */
class StepBuilder(val id: String) {
    var agent: Agent? = null
    var input: (WorkflowContext) -> Map<String, Any> = { emptyMap() }
    
    fun build(): WorkflowStep {
        // 简化实现
        return object : WorkflowStep {
            override val id: String = this@StepBuilder.id
            
            override fun execute(context: WorkflowContext): Map<String, Any> {
                return mapOf("result" to "这是步骤 $id 的结果")
            }
        }
    }
}

/**
 * 工作流上下文。
 */
interface WorkflowContext {
    val input: Map<String, Any>
    val steps: Map<String, StepResult>
}

/**
 * 步骤结果。
 */
interface StepResult {
    val output: Map<String, Any>
}

/**
 * 工作流步骤。
 */
interface WorkflowStep {
    val id: String
    
    fun execute(context: WorkflowContext): Map<String, Any>
}

/**
 * 工作流。
 */
interface Workflow {
    val id: String
    val description: String
    
    fun execute(input: Map<String, Any>): Map<String, Any>
}
