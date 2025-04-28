package ai.kastrax.a2x.adapter

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.model.A2AMessage
import ai.kastrax.a2a.model.CapabilityRequest as A2ACapabilityRequest
import ai.kastrax.a2a.model.InvokeRequest as A2AInvokeRequest
import ai.kastrax.a2a.model.QueryRequest as A2AQueryRequest
import ai.kastrax.a2x.A2X
import ai.kastrax.a2x.EntityAdapter
import ai.kastrax.a2x.entity.A2XException
import ai.kastrax.a2x.entity.Entity
import ai.kastrax.a2x.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * A2A 代理适配器，将 A2A 代理适配为 A2X 实体
 */
class A2AEntityAdapter : EntityAdapter {
    /**
     * A2X 实例
     */
    private val a2x = A2X.getInstance()

    /**
     * 适配 A2A 代理为 A2X 实体
     */
    override fun adapt(obj: Any): Entity {
        if (obj !is A2AAgent) {
            throw IllegalArgumentException("Object is not an A2AAgent: ${obj.javaClass.name}")
        }
        
        return A2AEntityImpl(obj)
    }

    /**
     * A2A 实体实现
     */
    private inner class A2AEntityImpl(
        private val a2aAgent: A2AAgent,
        private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) : Entity {
        /**
         * 实体卡片
         */
        private val entityCard: EntityCard by lazy {
            val agentCard = a2aAgent.getAgentCard()
            
            EntityCard(
                id = agentCard.id,
                name = agentCard.name,
                description = agentCard.description,
                version = agentCard.version,
                type = EntityType.AGENT,
                endpoint = agentCard.endpoint,
                capabilities = agentCard.capabilities.map { capability ->
                    Capability(
                        id = capability.id,
                        name = capability.name,
                        description = capability.description,
                        version = capability.version,
                        parameters = capability.parameters.map { parameter ->
                            Parameter(
                                name = parameter.name,
                                type = parameter.type,
                                description = parameter.description,
                                required = parameter.required,
                                defaultValue = parameter.defaultValue,
                                metadata = parameter.metadata
                            )
                        },
                        returnType = capability.returnType,
                        metadata = capability.metadata
                    )
                },
                authentication = Authentication(
                    type = when (agentCard.authentication.type) {
                        ai.kastrax.a2a.model.AuthType.NONE -> AuthenticationType.NONE
                        ai.kastrax.a2a.model.AuthType.API_KEY -> AuthenticationType.API_KEY
                        ai.kastrax.a2a.model.AuthType.OAUTH2 -> AuthenticationType.OAUTH2
                        ai.kastrax.a2a.model.AuthType.JWT -> AuthenticationType.JWT
                        ai.kastrax.a2a.model.AuthType.BASIC -> AuthenticationType.BASIC
                        else -> AuthenticationType.OTHER
                    },
                    metadata = agentCard.authentication.metadata
                ),
                metadata = agentCard.metadata
            )
        }

        override fun getEntityCard(): EntityCard = entityCard

        override fun getCapabilities(): List<Capability> = entityCard.capabilities

        override suspend fun invoke(request: InvokeRequest): InvokeResponse {
            // 将 A2X 请求转换为 A2A 请求
            val a2aRequest = A2AInvokeRequest(
                id = request.id,
                capabilityId = request.capabilityId,
                parameters = request.parameters,
                metadata = request.metadata
            )
            
            // 调用 A2A 代理
            val a2aResponse = a2aAgent.invoke(a2aRequest)
            
            // 将 A2A 响应转换为 A2X 响应
            return InvokeResponse(
                id = a2aResponse.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.AGENT
                ),
                target = request.source,
                result = a2aResponse.result,
                metadata = a2aResponse.metadata
            )
        }

        override suspend fun query(request: QueryRequest): QueryResponse {
            // 将 A2X 请求转换为 A2A 请求
            val a2aRequest = A2AQueryRequest(
                id = request.id,
                queryType = request.queryType,
                parameters = request.parameters,
                metadata = request.metadata
            )
            
            // 调用 A2A 代理
            val a2aResponse = a2aAgent.query(a2aRequest)
            
            // 将 A2A 响应转换为 A2X 响应
            return QueryResponse(
                id = a2aResponse.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.AGENT
                ),
                target = request.source,
                result = a2aResponse.result,
                metadata = a2aResponse.metadata
            )
        }

        override suspend fun processMessage(message: A2XMessage): A2XMessage {
            return when (message) {
                is CapabilityRequest -> handleCapabilityRequest(message)
                is InvokeRequest -> invoke(message)
                is QueryRequest -> query(message)
                else -> ErrorMessage(
                    id = message.id,
                    source = EntityReference(
                        id = entityCard.id,
                        type = EntityType.AGENT
                    ),
                    target = message.source,
                    code = "unsupported_message_type",
                    message = "Unsupported message type: ${message.type}"
                )
            }
        }

        override suspend fun sendEvent(event: EventMessage) {
            // A2A 代理不支持事件，所以这里不做任何处理
        }

        override fun subscribeToEvents(eventTypes: List<String>): Flow<EventMessage> {
            // 订阅 A2X 事件流，过滤出目标是当前实体的事件
            return a2x.eventFlow.filter { event ->
                event.target.id == entityCard.id || event.target.id == "*"
            }.filter { event ->
                eventTypes.isEmpty() || eventTypes.contains(event.eventType)
            }
        }

        override fun start() {
            a2aAgent.start()
        }

        override fun stop() {
            a2aAgent.stop()
        }

        /**
         * 处理能力请求
         */
        private fun handleCapabilityRequest(request: CapabilityRequest): CapabilityResponse {
            val capabilities = if (request.capabilityId != null) {
                entityCard.capabilities.filter { it.id == request.capabilityId }
            } else {
                entityCard.capabilities
            }
            
            return CapabilityResponse(
                id = request.id,
                source = EntityReference(
                    id = entityCard.id,
                    type = EntityType.AGENT
                ),
                target = request.source,
                capabilities = capabilities,
                metadata = request.metadata
            )
        }
    }
}
