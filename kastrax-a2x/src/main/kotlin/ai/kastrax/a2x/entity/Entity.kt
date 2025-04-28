package ai.kastrax.a2x.entity

import ai.kastrax.a2x.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 实体接口，定义实体的基本功能
 */
interface Entity {
    /**
     * 获取实体卡片
     */
    fun getEntityCard(): EntityCard

    /**
     * 获取实体能力
     */
    fun getCapabilities(): List<Capability>

    /**
     * 调用实体能力
     */
    suspend fun invoke(request: InvokeRequest): InvokeResponse

    /**
     * 查询实体状态
     */
    suspend fun query(request: QueryRequest): QueryResponse

    /**
     * 处理 A2X 消息
     */
    suspend fun processMessage(message: A2XMessage): A2XMessage

    /**
     * 发送事件
     */
    suspend fun sendEvent(event: EventMessage)

    /**
     * 订阅事件
     */
    fun subscribeToEvents(eventTypes: List<String>): Flow<EventMessage>

    /**
     * 启动实体
     */
    fun start()

    /**
     * 停止实体
     */
    fun stop()
}

/**
 * A2X 异常
 */
class A2XException(val code: String, override val message: String) : Exception(message)
