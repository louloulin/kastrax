package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.EmbeddingGenerator
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.MemoryProcessorOptions
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.SemanticMemory
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.api.SemanticSearchResult
import ai.kastrax.memory.api.VectorStorage
import ai.kastrax.memory.api.WorkingMemory
import ai.kastrax.memory.api.WorkingMemoryConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 增强型内存实现，支持工作内存、语义搜索和记忆处理器。
 */
class EnhancedMemory(
    private val storage: Any,
    private val lastMessagesCount: Int = 10,
    private val semanticRecallEnabled: Boolean = false,
    private val embeddingGenerator: EmbeddingGenerator? = null,
    private val vectorStorage: VectorStorage? = null,
    private val processors: List<MemoryProcessor> = emptyList(),
    private val workingMemoryConfig: WorkingMemoryConfig? = null
) : Memory, KastraXBase(component = "MEMORY", name = "enhanced") {
    private val mutex = Mutex()
    private val threads = mutableMapOf<String, String>() // threadId -> title
    private val messages = mutableMapOf<String, MutableList<MemoryMessage>>() // threadId -> messages
    
    private val workingMemory: WorkingMemory? = if (workingMemoryConfig?.enabled == true) {
        InMemoryWorkingMemory()
    } else {
        null
    }
    
    private val semanticMemory: SemanticMemory? = if (semanticRecallEnabled && embeddingGenerator != null && vectorStorage != null) {
        InMemorySemanticMemory(embeddingGenerator, vectorStorage)
    } else {
        null
    }
    
    override suspend fun saveMessage(message: Message, threadId: String): String {
        val messageId = UUID.randomUUID().toString()
        
        // 创建内存消息
        val memoryMessage = MemoryMessage(
            id = messageId,
            threadId = threadId,
            message = message,
            createdAt = Clock.System.now()
        )
        
        // 保存消息
        mutex.withLock {
            val threadMessages = messages.getOrPut(threadId) { mutableListOf() }
            threadMessages.add(memoryMessage)
        }
        
        // 如果启用了语义内存，保存到语义内存
        semanticMemory?.saveMessage(message, threadId)
        
        return messageId
    }
    
    override suspend fun getMessages(
        threadId: String,
        limit: Int,
        processors: List<MemoryProcessor>?
    ): List<MemoryMessage> {
        // 获取消息
        val threadMessages = mutex.withLock {
            messages[threadId]?.sortedBy { it.createdAt } ?: emptyList()
        }
        
        // 如果没有处理器，直接返回最近的消息
        if (processors.isNullOrEmpty() && this.processors.isEmpty()) {
            return threadMessages.takeLast(limit)
        }
        
        // 应用处理器
        val allProcessors = (processors ?: emptyList()) + this.processors
        var processedMessages = threadMessages
        
        for (processor in allProcessors) {
            processedMessages = processor.process(
                processedMessages,
                MemoryProcessorOptions()
            )
        }
        
        // 返回处理后的消息
        return processedMessages.takeLast(limit)
    }
    
    override suspend fun searchMessages(query: String, threadId: String, limit: Int): List<MemoryMessage> {
        // 如果启用了语义内存，使用语义搜索
        if (semanticRecallEnabled && semanticMemory != null) {
            return semanticMemory.getSemanticRecallMessages(
                query = query,
                threadId = threadId,
                config = SemanticRecallConfig(topK = limit)
            )
        }
        
        // 否则，使用简单的关键词搜索
        return mutex.withLock {
            messages[threadId]?.filter { 
                it.message.content.contains(query, ignoreCase = true) 
            }?.sortedBy { it.createdAt }?.take(limit) ?: emptyList()
        }
    }
    
    override suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig
    ): List<SemanticSearchResult> {
        return semanticMemory?.semanticSearch(query, threadId, config) ?: emptyList()
    }
    
    override suspend fun createThread(title: String?): String {
        val threadId = UUID.randomUUID().toString()
        
        mutex.withLock {
            threads[threadId] = title ?: ""
            messages[threadId] = mutableListOf()
        }
        
        // 如果启用了工作内存，初始化工作内存
        if (workingMemory != null && workingMemoryConfig?.enabled == true) {
            workingMemory.updateWorkingMemory(threadId, workingMemoryConfig.template)
        }
        
        return threadId
    }
    
    /**
     * 获取工作内存系统消息。
     */
    suspend fun getWorkingMemorySystemMessage(threadId: String): String? {
        return workingMemory?.getSystemMessage(threadId, workingMemoryConfig)
    }
    
    /**
     * 获取工作内存工具。
     */
    fun getWorkingMemoryTools(): Map<String, Any> {
        return workingMemory?.getTools(workingMemoryConfig) ?: emptyMap()
    }
}

/**
 * 增强型内存构建器实现。
 */
class EnhancedMemoryBuilder : MemoryBuilder {
    private var storage: Any? = null
    private var lastMessagesCount: Int = 10
    private var semanticRecallEnabled: Boolean = false
    private var embeddingGenerator: EmbeddingGenerator? = null
    private var vectorStorage: VectorStorage? = null
    private val processors = mutableListOf<MemoryProcessor>()
    private var workingMemoryConfig: WorkingMemoryConfig? = null
    
    override fun storage(storage: Any): MemoryBuilder {
        this.storage = storage
        return this
    }
    
    override fun lastMessages(count: Int): MemoryBuilder {
        this.lastMessagesCount = count
        return this
    }
    
    override fun semanticRecall(enabled: Boolean): MemoryBuilder {
        this.semanticRecallEnabled = enabled
        return this
    }
    
    override fun embeddingGenerator(generator: EmbeddingGenerator): MemoryBuilder {
        this.embeddingGenerator = generator
        return this
    }
    
    override fun vectorStorage(storage: VectorStorage): MemoryBuilder {
        this.vectorStorage = storage
        return this
    }
    
    override fun processor(processor: MemoryProcessor): MemoryBuilder {
        this.processors.add(processor)
        return this
    }
    
    override fun workingMemory(config: WorkingMemoryConfig): MemoryBuilder {
        this.workingMemoryConfig = config
        return this
    }
    
    override fun build(): Memory {
        return EnhancedMemory(
            storage = storage ?: Any(),
            lastMessagesCount = lastMessagesCount,
            semanticRecallEnabled = semanticRecallEnabled,
            embeddingGenerator = embeddingGenerator,
            vectorStorage = vectorStorage,
            processors = processors,
            workingMemoryConfig = workingMemoryConfig
        )
    }
}

/**
 * 创建增强型内存的DSL函数。
 */
fun enhancedMemory(init: EnhancedMemoryBuilder.() -> Unit): Memory {
    val builder = EnhancedMemoryBuilder()
    builder.init()
    return builder.build()
}
