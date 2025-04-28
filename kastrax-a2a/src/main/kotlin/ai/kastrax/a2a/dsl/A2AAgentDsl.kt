package ai.kastrax.a2a.dsl

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.agent.A2AAgentImpl
import ai.kastrax.a2a.model.*
import ai.kastrax.core.agent.Agent
import java.util.UUID

/**
 * A2A 代理 DSL 构建器
 */
class A2AAgentBuilder {
    /**
     * 代理 ID
     */
    var id: String = UUID.randomUUID().toString()
    
    /**
     * 代理名称
     */
    var name: String = "A2A Agent"
    
    /**
     * 代理描述
     */
    var description: String = ""
    
    /**
     * 代理版本
     */
    var version: String = "1.0.0"
    
    /**
     * 代理端点
     */
    var endpoint: String = "/a2a/v1/agents"
    
    /**
     * 基础代理
     */
    var baseAgent: Agent? = null
    
    /**
     * 代理能力列表
     */
    private val capabilities = mutableListOf<CapabilityBuilder>()
    
    /**
     * 代理认证
     */
    private var authentication = AuthenticationBuilder()
    
    /**
     * 代理元数据
     */
    private val metadata = mutableMapOf<String, String>()
    
    /**
     * 添加代理能力
     */
    fun capability(init: CapabilityBuilder.() -> Unit) {
        val builder = CapabilityBuilder()
        builder.init()
        capabilities.add(builder)
    }
    
    /**
     * 配置代理认证
     */
    fun authentication(init: AuthenticationBuilder.() -> Unit) {
        authentication.init()
    }
    
    /**
     * 添加代理元数据
     */
    fun metadata(key: String, value: String) {
        metadata[key] = value
    }
    
    /**
     * 构建 A2A 代理
     */
    fun build(): A2AAgent {
        requireNotNull(baseAgent) { "Base agent is required" }
        
        val agentCard = AgentCard(
            id = id,
            name = name,
            description = description,
            version = version,
            endpoint = "$endpoint/$id",
            capabilities = capabilities.map { it.build() },
            authentication = authentication.build(),
            metadata = metadata
        )
        
        return A2AAgentImpl(agentCard, baseAgent!!)
    }
}

/**
 * 代理能力 DSL 构建器
 */
class CapabilityBuilder {
    /**
     * 能力 ID
     */
    var id: String = UUID.randomUUID().toString()
    
    /**
     * 能力名称
     */
    var name: String = ""
    
    /**
     * 能力描述
     */
    var description: String = ""
    
    /**
     * 能力返回类型
     */
    var returnType: String = "json"
    
    /**
     * 能力参数列表
     */
    private val parameters = mutableListOf<ParameterBuilder>()
    
    /**
     * 能力示例列表
     */
    private val examples = mutableListOf<ExampleBuilder>()
    
    /**
     * 添加能力参数
     */
    fun parameter(init: ParameterBuilder.() -> Unit) {
        val builder = ParameterBuilder()
        builder.init()
        parameters.add(builder)
    }
    
    /**
     * 添加能力示例
     */
    fun example(init: ExampleBuilder.() -> Unit) {
        val builder = ExampleBuilder()
        builder.init()
        examples.add(builder)
    }
    
    /**
     * 构建代理能力
     */
    fun build(): Capability {
        return Capability(
            id = id,
            name = name,
            description = description,
            parameters = parameters.map { it.build() },
            returnType = returnType,
            examples = examples.map { it.build() }
        )
    }
}

/**
 * 能力参数 DSL 构建器
 */
class ParameterBuilder {
    /**
     * 参数名称
     */
    var name: String = ""
    
    /**
     * 参数类型
     */
    var type: String = "string"
    
    /**
     * 参数描述
     */
    var description: String = ""
    
    /**
     * 参数是否必需
     */
    var required: Boolean = false
    
    /**
     * 参数 Schema
     */
    var schema: kotlinx.serialization.json.JsonObject? = null
    
    /**
     * 构建能力参数
     */
    fun build(): Parameter {
        return Parameter(
            name = name,
            type = type,
            description = description,
            required = required,
            schema = schema
        )
    }
}

/**
 * 能力示例 DSL 构建器
 */
class ExampleBuilder {
    /**
     * 示例输入
     */
    private val input = mutableMapOf<String, String>()
    
    /**
     * 示例输出
     */
    private val output = mutableMapOf<String, String>()
    
    /**
     * 示例描述
     */
    var description: String? = null
    
    /**
     * 添加示例输入
     */
    fun input(key: String, value: String) {
        input[key] = value
    }
    
    /**
     * 添加示例输出
     */
    fun output(key: String, value: String) {
        output[key] = value
    }
    
    /**
     * 构建能力示例
     */
    fun build(): Example {
        return Example(
            input = input,
            output = output,
            description = description
        )
    }
}

/**
 * 代理认证 DSL 构建器
 */
class AuthenticationBuilder {
    /**
     * 认证类型
     */
    var type: AuthType = AuthType.NONE
    
    /**
     * 认证详情
     */
    private val details = mutableMapOf<String, String>()
    
    /**
     * 添加认证详情
     */
    fun detail(key: String, value: String) {
        details[key] = value
    }
    
    /**
     * 构建代理认证
     */
    fun build(): Authentication {
        return Authentication(
            type = type,
            details = details
        )
    }
}

/**
 * 创建 A2A 代理的 DSL 函数
 */
fun a2aAgent(init: A2AAgentBuilder.() -> Unit): A2AAgent {
    val builder = A2AAgentBuilder()
    builder.init()
    return builder.build()
}
