package ai.kastrax.a2x

import ai.kastrax.a2a.A2A
import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2x.adapter.*
import ai.kastrax.a2x.adapter.APIConfig
import ai.kastrax.a2x.client.A2XClient
import ai.kastrax.a2x.client.A2XClientConfig
import ai.kastrax.a2x.discovery.A2XDiscoveryService
import ai.kastrax.a2x.entity.Entity
import ai.kastrax.a2x.model.*
import ai.kastrax.a2x.security.A2XSecurityService
import ai.kastrax.a2x.semantic.SemanticService
import ai.kastrax.a2x.server.A2XServerConfig
import ai.kastrax.core.agent.Agent
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A2X 模块的主入口类
 */
class A2X private constructor() {
    /**
     * 单例实例
     */
    companion object {
        private val instance = A2X()

        /**
         * 获取单例实例
         */
        fun getInstance(): A2X = instance
    }

    /**
     * A2A 实例
     */
    private val a2a = A2A.getInstance()

    /**
     * 实体适配器
     */
    private val entityAdapters = mutableMapOf<EntityType, EntityAdapter>()

    /**
     * 实体发现服务
     */
    private val discoveryService = A2XDiscoveryService()

    /**
     * 安全服务
     */
    private val securityService = A2XSecurityService()

    /**
     * 语义服务
     */
    private val semanticService = SemanticService()

    /**
     * 协程作用域
     */
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * 已注册的实体
     */
    private val entities = ConcurrentHashMap<String, Entity>()

    /**
     * 服务器实例
     */
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    /**
     * 事件流
     */
    private val _eventFlow = MutableSharedFlow<EventMessage>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        // 注册默认适配器
        registerEntityAdapter(EntityType.AGENT, A2AEntityAdapter())
        registerEntityAdapter(EntityType.SYSTEM, SystemEntityAdapter())
        registerEntityAdapter(EntityType.SYSTEM, DatabaseEntityAdapter())
        registerEntityAdapter(EntityType.SYSTEM, APIEntityAdapter())
    }

    /**
     * 注册实体适配器
     */
    fun registerEntityAdapter(entityType: EntityType, adapter: EntityAdapter) {
        entityAdapters[entityType] = adapter
    }

    /**
     * 获取实体适配器
     */
    fun getEntityAdapter(entityType: EntityType): EntityAdapter? {
        return entityAdapters[entityType]
    }

    /**
     * 适配 kastrax 代理为 A2X 实体
     */
    fun adaptAgent(agent: Agent): Entity {
        val a2aAgent = a2a.adaptAgent(agent)
        return adaptA2AAgent(a2aAgent)
    }

    /**
     * 适配 A2A 代理为 A2X 实体
     */
    fun adaptA2AAgent(a2aAgent: A2AAgent): Entity {
        val adapter = getEntityAdapter(EntityType.AGENT) ?: throw IllegalStateException("No adapter registered for AGENT entity type")
        return adapter.adapt(a2aAgent)
    }

    /**
     * 适配数据库连接器为 A2X 实体
     */
    fun adaptDatabaseConnector(databaseConnector: ai.kastrax.datasource.common.DatabaseConnector): Entity {
        val adapter = getEntityAdapter(EntityType.SYSTEM) ?: throw IllegalStateException("No adapter registered for SYSTEM entity type")
        return adapter.adapt(databaseConnector)
    }

    /**
     * 适配 Web API 为 A2X 实体
     */
    fun adaptAPI(apiConfig: APIConfig): Entity {
        val adapter = getEntityAdapter(EntityType.SYSTEM) ?: throw IllegalStateException("No adapter registered for SYSTEM entity type")
        return adapter.adapt(apiConfig)
    }

    /**
     * 注册实体
     */
    fun registerEntity(entity: Entity): Entity {
        val entityCard = entity.getEntityCard()
        entities[entityCard.id] = entity
        return entity
    }

    /**
     * 注销实体
     */
    fun unregisterEntity(entityId: String) {
        entities.remove(entityId)
    }

    /**
     * 获取实体
     */
    fun getEntity(entityId: String): Entity? {
        return entities[entityId]
    }

    /**
     * 获取所有实体
     */
    fun getAllEntities(): List<Entity> {
        return entities.values.toList()
    }

    /**
     * 获取指定类型的所有实体
     */
    fun getEntitiesByType(entityType: EntityType): List<Entity> {
        return entities.values.filter { it.getEntityCard().type == entityType }
    }

    /**
     * 创建 A2X 客户端
     */
    fun createClient(serverUrl: String, apiKey: String? = null): A2XClient {
        return A2XClient(A2XClientConfig(serverUrl = serverUrl, apiKey = apiKey))
    }

    /**
     * 启动 A2X 服务器
     */
    fun startServer(config: A2XServerConfig = A2XServerConfig()) {
        if (server != null) {
            return
        }

        // 记录服务器启动事件
        println("Starting A2X server on ${config.host}:${config.port}")

        server = embeddedServer(Netty, port = config.port, host = config.host) {
            // 配置 A2X 服务器
            // 注释掉这一行，因为我们还没有实现这个函数
            // ai.kastrax.a2x.server.configureA2XServer(entities, securityService)
        }

        server?.start(wait = false)

        // 记录服务器启动成功事件
        println("A2X server started successfully")
    }

    /**
     * 停止 A2X 服务器
     */
    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
        println("A2X server stopped")
    }

    /**
     * 添加服务器到发现服务
     */
    fun addServerToDiscovery(serverUrl: String) {
        discoveryService.registerServer(serverUrl)
    }

    /**
     * 从发现服务移除服务器
     */
    fun removeServerFromDiscovery(serverUrl: String) {
        discoveryService.unregisterServer(serverUrl)
    }

    /**
     * 发送消息
     */
    suspend fun sendMessage(message: A2XMessage): A2XMessage {
        val targetEntity = entities[message.target.id] ?: throw IllegalArgumentException("Target entity not found: ${message.target.id}")
        return targetEntity.processMessage(message)
    }

    /**
     * 发送事件
     */
    suspend fun sendEvent(event: EventMessage) {
        _eventFlow.emit(event)

        // 如果指定了目标实体，则直接发送给目标实体
        if (event.target.id != "*") {
            val targetEntity = entities[event.target.id]
            targetEntity?.sendEvent(event)
        }

        // 否则，广播给所有实体
        else {
            entities.values.forEach { entity ->
                entity.sendEvent(event)
            }
        }
    }

    /**
     * 创建实体引用
     */
    fun createEntityReference(entityId: String, entityType: EntityType, endpoint: String? = null): EntityReference {
        return EntityReference(
            id = entityId,
            type = entityType,
            endpoint = endpoint
        )
    }

    /**
     * 创建本地实体引用
     */
    fun createLocalEntityReference(entityId: String, entityType: EntityType): EntityReference {
        return createEntityReference(entityId, entityType, null)
    }

    /**
     * 获取语义服务
     */
    fun getSemanticService(): SemanticService {
        return semanticService
    }

    /**
     * 处理消息
     */
    fun processMessage(text: String, sessionId: String? = null): ai.kastrax.a2x.semantic.MessageProcessingResult {
        return semanticService.processMessage(text, sessionId)
    }

    /**
     * 生成响应
     */
    fun generateResponse(result: ai.kastrax.a2x.semantic.MessageProcessingResult, sessionId: String? = null): ai.kastrax.a2x.semantic.ResponseGenerationResult {
        return semanticService.generateResponse(result, sessionId)
    }

    /**
     * 创建会话
     */
    fun createSession(contextId: String, entityId: String): ai.kastrax.a2x.semantic.Session? {
        return semanticService.createSession(contextId, entityId)
    }

    /**
     * 创建上下文
     */
    fun createContext(name: String, description: String, type: String): ai.kastrax.a2x.semantic.Context {
        return semanticService.createContext(name, description, type)
    }
}

/**
 * 实体适配器接口
 */
interface EntityAdapter {
    /**
     * 适配对象为 A2X 实体
     */
    fun adapt(obj: Any): Entity
}

/**
 * 创建 A2X 实例的 DSL 函数
 */
fun a2x(init: A2XDslBuilder.() -> Unit): A2X {
    val builder = A2XDslBuilder()
    builder.init()
    return builder.build()
}

/**
 * A2X DSL 构建器
 */
class A2XDslBuilder {
    /**
     * A2X 实例
     */
    private val a2x = A2X.getInstance()

    /**
     * 服务器配置
     */
    private var serverConfig: A2XServerConfig? = null

    /**
     * 是否启动服务器
     */
    private var startServer = false

    /**
     * 配置服务器
     */
    fun server(init: ServerConfigBuilder.() -> Unit) {
        val builder = ServerConfigBuilder()
        builder.init()
        serverConfig = builder.build()
        startServer = true
    }

    /**
     * 注册代理
     */
    fun agent(agent: Agent) {
        val entity = a2x.adaptAgent(agent)
        a2x.registerEntity(entity)
    }

    /**
     * 注册 A2A 代理
     */
    fun a2aAgent(a2aAgent: A2AAgent) {
        val entity = a2x.adaptA2AAgent(a2aAgent)
        a2x.registerEntity(entity)
    }

    /**
     * 注册数据库连接器
     */
    fun database(databaseConnector: ai.kastrax.datasource.common.DatabaseConnector) {
        val entity = a2x.adaptDatabaseConnector(databaseConnector)
        a2x.registerEntity(entity)
    }

    /**
     * 注册 Web API
     */
    fun api(apiConfig: APIConfig) {
        val entity = a2x.adaptAPI(apiConfig)
        a2x.registerEntity(entity)
    }

    /**
     * 注册实体
     */
    fun entity(entity: Entity) {
        a2x.registerEntity(entity)
    }

    /**
     * 添加服务器到发现服务
     */
    fun discovery(serverUrl: String) {
        a2x.addServerToDiscovery(serverUrl)
    }

    /**
     * 配置语义
     */
    fun semantic(init: SemanticConfigBuilder.() -> Unit) {
        val builder = SemanticConfigBuilder(a2x)
        builder.init()
    }

    /**
     * 构建 A2X 实例
     */
    fun build(): A2X {
        // 启动服务器
        if (startServer) {
            a2x.startServer(serverConfig ?: A2XServerConfig())
        }

        return a2x
    }
}

/**
 * 服务器配置构建器
 */
class ServerConfigBuilder {
    /**
     * 服务器端口
     */
    var port: Int = 8080

    /**
     * 服务器主机
     */
    var host: String = "0.0.0.0"

    /**
     * 是否启用 CORS
     */
    var enableCors: Boolean = true

    /**
     * 构建服务器配置
     */
    fun build(): A2XServerConfig {
        return A2XServerConfig(
            port = port,
            host = host,
            enableCors = enableCors
        )
    }
}

/**
 * 语义配置构建器
 */
class SemanticConfigBuilder(private val a2x: A2X) {
    /**
     * 添加本体
     */
    fun ontology(init: OntologyBuilder.() -> Unit) {
        val builder = OntologyBuilder(a2x.getSemanticService().ontologyManager)
        builder.init()
    }

    /**
     * 添加意图
     */
    fun intent(name: String, description: String, init: IntentBuilder.() -> Unit) {
        val intent = a2x.getSemanticService().intentRecognizer.createIntent(name, description)
        val builder = IntentBuilder(a2x.getSemanticService().intentRecognizer, intent.id)
        builder.init()
    }

    /**
     * 添加实体类型
     */
    fun entityType(name: String, description: String, init: EntityTypeBuilder.() -> Unit) {
        val entityType = a2x.getSemanticService().entityResolver.createEntityType(name, description)
        val builder = EntityTypeBuilder(a2x.getSemanticService().entityResolver, entityType.id)
        builder.init()
    }

    /**
     * 创建上下文
     */
    fun context(name: String, description: String, type: String): ai.kastrax.a2x.semantic.Context {
        return a2x.createContext(name, description, type)
    }
}

/**
 * 本体构建器
 */
class OntologyBuilder(private val ontologyManager: ai.kastrax.a2x.semantic.OntologyManager) {
    /**
     * 本体 ID
     */
    var id: String = "ontology-${java.util.UUID.randomUUID()}"

    /**
     * 本体名称
     */
    var name: String = ""

    /**
     * 本体描述
     */
    var description: String = ""

    /**
     * 本体版本
     */
    var version: String = "1.0.0"

    /**
     * 本体概念列表
     */
    private val concepts = mutableListOf<ai.kastrax.a2x.semantic.Concept>()

    /**
     * 本体关系列表
     */
    private val relations = mutableListOf<ai.kastrax.a2x.semantic.Relation>()

    /**
     * 添加概念
     */
    fun concept(init: ConceptBuilder.() -> Unit) {
        val builder = ConceptBuilder()
        builder.init()
        concepts.add(builder.build())
    }

    /**
     * 添加关系
     */
    fun relation(init: RelationBuilder.() -> Unit) {
        val builder = RelationBuilder()
        builder.init()
        relations.add(builder.build())
    }

    /**
     * 构建本体
     */
    fun build(): ai.kastrax.a2x.semantic.Ontology {
        val ontology = ai.kastrax.a2x.semantic.Ontology(
            id = id,
            name = name,
            description = description,
            version = version,
            concepts = concepts,
            relations = relations
        )

        ontologyManager.registerOntology(ontology)
        return ontology
    }
}

/**
 * 概念构建器
 */
class ConceptBuilder {
    /**
     * 概念 ID
     */
    var id: String = "concept-${java.util.UUID.randomUUID()}"

    /**
     * 概念名称
     */
    var name: String = ""

    /**
     * 概念描述
     */
    var description: String = ""

    /**
     * 概念类型
     */
    var type: String = "entity"

    /**
     * 概念属性列表
     */
    private val properties = mutableListOf<ai.kastrax.a2x.semantic.Property>()

    /**
     * 添加属性
     */
    fun property(init: PropertyBuilder.() -> Unit) {
        val builder = PropertyBuilder()
        builder.init()
        properties.add(builder.build())
    }

    /**
     * 构建概念
     */
    fun build(): ai.kastrax.a2x.semantic.Concept {
        return ai.kastrax.a2x.semantic.Concept(
            id = id,
            name = name,
            description = description,
            type = type,
            properties = properties
        )
    }
}

/**
 * 属性构建器
 */
class PropertyBuilder {
    /**
     * 属性名称
     */
    var name: String = ""

    /**
     * 属性类型
     */
    var type: String = "string"

    /**
     * 属性描述
     */
    var description: String = ""

    /**
     * 属性是否必需
     */
    var required: Boolean = false

    /**
     * 属性默认值
     */
    var defaultValue: String? = null

    /**
     * 构建属性
     */
    fun build(): ai.kastrax.a2x.semantic.Property {
        return ai.kastrax.a2x.semantic.Property(
            name = name,
            type = type,
            description = description,
            required = required,
            defaultValue = defaultValue
        )
    }
}

/**
 * 关系构建器
 */
class RelationBuilder {
    /**
     * 关系 ID
     */
    var id: String = "relation-${java.util.UUID.randomUUID()}"

    /**
     * 关系名称
     */
    var name: String = ""

    /**
     * 关系描述
     */
    var description: String = ""

    /**
     * 关系类型
     */
    var type: String = "association"

    /**
     * 源概念 ID
     */
    var sourceConcept: String = ""

    /**
     * 目标概念 ID
     */
    var targetConcept: String = ""

    /**
     * 关系属性列表
     */
    private val properties = mutableListOf<ai.kastrax.a2x.semantic.Property>()

    /**
     * 添加属性
     */
    fun property(init: PropertyBuilder.() -> Unit) {
        val builder = PropertyBuilder()
        builder.init()
        properties.add(builder.build())
    }

    /**
     * 构建关系
     */
    fun build(): ai.kastrax.a2x.semantic.Relation {
        return ai.kastrax.a2x.semantic.Relation(
            id = id,
            name = name,
            description = description,
            type = type,
            sourceConcept = sourceConcept,
            targetConcept = targetConcept,
            properties = properties
        )
    }
}

/**
 * 意图构建器
 */
class IntentBuilder(private val intentRecognizer: ai.kastrax.a2x.semantic.IntentRecognizer, private val intentId: String) {
    /**
     * 添加精确模式
     */
    fun exactPattern(pattern: String) {
        intentRecognizer.addIntentPattern(intentId, "exact", pattern)
    }

    /**
     * 添加正则表达式模式
     */
    fun regexPattern(pattern: String) {
        intentRecognizer.addIntentPattern(intentId, "regex", pattern)
    }

    /**
     * 添加关键词模式
     */
    fun keywordPattern(keywords: List<String>) {
        intentRecognizer.addIntentPattern(intentId, "keyword", keywords.joinToString(","))
    }
}

/**
 * 实体类型构建器
 */
class EntityTypeBuilder(private val entityResolver: ai.kastrax.a2x.semantic.EntityResolver, private val entityTypeId: String) {
    /**
     * 添加正则表达式提取器
     */
    fun regexExtractor(name: String, pattern: String) {
        entityResolver.createRegexExtractor(entityTypeId, name, pattern)
    }

    /**
     * 添加字典提取器
     */
    fun dictionaryExtractor(name: String, entries: List<String>) {
        entityResolver.createDictionaryExtractor(entityTypeId, name, entries)
    }
}
