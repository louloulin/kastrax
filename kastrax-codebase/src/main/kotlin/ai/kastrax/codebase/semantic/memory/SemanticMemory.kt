package ai.kastrax.codebase.semantic.memory

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * 记忆类型
 */
enum class MemoryType {
    CODE_STRUCTURE,
    SYMBOL_DEFINITION,
    SYMBOL_REFERENCE,
    INHERITANCE,
    IMPLEMENTATION,
    CALL_HIERARCHY,
    IMPORT_DEPENDENCY,
    LIBRARY_USAGE,
    SEMANTIC_RELATION,
    IMPORT_STATEMENT,    // 导入语句记忆
    LIBRARY_API,         // 库API记忆
    SEMANTIC_SIMILARITY, // 语义相似度记忆
    LIBRARY_FUNCTION,    // 库函数记忆
    LIBRARY_CLASS,       // 库类记忆
    IMPORT_ANALYSIS,     // 导入分析记忆
    CUSTOM
}

/**
 * 记忆重要性级别
 */
enum class ImportanceLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    UNKNOWN
}

/**
 * 语义记忆
 *
 * @property id 唯一标识符
 * @property type 记忆类型
 * @property content 记忆内容
 * @property sourceElements 源代码元素列表
 * @property sourceSymbols 源符号节点列表
 * @property relatedMemories 相关记忆 ID 列表
 * @property importance 重要性级别
 * @property creationTime 创建时间
 * @property lastAccessTime 最后访问时间
 * @property accessCount 访问次数
 * @property metadata 元数据
 */
data class SemanticMemory(
    val id: String = UUID.randomUUID().toString(),
    val type: MemoryType,
    val content: String,
    val sourceElements: List<CodeElement> = emptyList(),
    val sourceSymbols: List<SymbolNode> = emptyList(),
    val relatedMemories: MutableList<String> = mutableListOf(),
    var importance: ImportanceLevel = ImportanceLevel.MEDIUM,
    val creationTime: Instant = Instant.now(),
    var lastAccessTime: Instant = Instant.now(),
    var accessCount: Int = 0,
    val metadata: MutableMap<String, Any> = mutableMapOf(),
    val semanticVector: FloatArray? = null,
    val semanticScore: Float = 0.0f
) {
    /**
     * 访问记忆
     */
    fun access() {
        lastAccessTime = Instant.now()
        accessCount++
    }

    /**
     * 添加相关记忆
     *
     * @param memoryId 记忆 ID
     */
    fun addRelatedMemory(memoryId: String) {
        if (!relatedMemories.contains(memoryId)) {
            relatedMemories.add(memoryId)
        }
    }

    /**
     * 移除相关记忆
     *
     * @param memoryId 记忆 ID
     * @return 是否成功移除
     */
    fun removeRelatedMemory(memoryId: String): Boolean {
        return relatedMemories.remove(memoryId)
    }

    /**
     * 获取记忆的简短描述
     *
     * @return 简短描述
     */
    fun getShortDescription(): String {
        return "${type.name.lowercase()}: ${content.take(100)}${if (content.length > 100) "..." else ""}"
    }

    /**
     * 获取记忆的详细描述
     *
     * @return 详细描述
     */
    fun getDetailedDescription(): String {
        val sb = StringBuilder()

        sb.appendLine("Memory ID: $id")
        sb.appendLine("Type: ${type.name.lowercase()}")
        sb.appendLine("Content: $content")
        sb.appendLine("Importance: ${importance.name.lowercase()}")
        sb.appendLine("Creation Time: $creationTime")
        sb.appendLine("Last Access Time: $lastAccessTime")
        sb.appendLine("Access Count: $accessCount")

        if (sourceElements.isNotEmpty()) {
            sb.appendLine("Source Elements: ${sourceElements.size}")
            sourceElements.take(3).forEach { element ->
                sb.appendLine("  - ${element.name} (${element.type.name.lowercase()})")
            }
            if (sourceElements.size > 3) {
                sb.appendLine("  - ... and ${sourceElements.size - 3} more")
            }
        }

        if (sourceSymbols.isNotEmpty()) {
            sb.appendLine("Source Symbols: ${sourceSymbols.size}")
            sourceSymbols.take(3).forEach { symbol ->
                sb.appendLine("  - ${symbol.name} (${symbol.type.name.lowercase()})")
            }
            if (sourceSymbols.size > 3) {
                sb.appendLine("  - ... and ${sourceSymbols.size - 3} more")
            }
        }

        if (relatedMemories.isNotEmpty()) {
            sb.appendLine("Related Memories: ${relatedMemories.size}")
        }

        if (metadata.isNotEmpty()) {
            sb.appendLine("Metadata:")
            metadata.entries.take(5).forEach { (key, value) ->
                sb.appendLine("  - $key: $value")
            }
            if (metadata.size > 5) {
                sb.appendLine("  - ... and ${metadata.size - 5} more")
            }
        }

        return sb.toString()
    }
}

/**
 * 语义记忆存储
 *
 * @property name 存储名称
 */
class SemanticMemoryStore(
    val name: String
) {
    // 记忆映射
    private val memories = ConcurrentHashMap<String, SemanticMemory>()

    // 类型索引
    private val typeIndex = ConcurrentHashMap<MemoryType, MutableSet<String>>()

    // 重要性索引
    private val importanceIndex = ConcurrentHashMap<ImportanceLevel, MutableSet<String>>()

    // 代码元素索引
    private val elementIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // 符号索引
    private val symbolIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // 文件路径索引
    private val filePathIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // 相关记忆索引
    private val relatedMemoriesIndex = ConcurrentHashMap<String, MutableSet<String>>()

    // 语义关系索引
    private val relationshipIndex = ConcurrentHashMap<String, MutableSet<SemanticRelationship>>()

    // 语义关系类型索引
    private val relationshipTypeIndex = ConcurrentHashMap<SemanticRelationshipType, MutableSet<String>>()

    // 读写锁
    private val lock = ReentrantReadWriteLock()

    /**
     * 添加记忆
     *
     * @param memory 语义记忆
     * @return 是否成功添加
     */
    fun addMemory(memory: SemanticMemory): Boolean {
        lock.write {
            // 检查记忆是否已存在
            if (memories.containsKey(memory.id)) {
                return false
            }

            // 添加记忆
            memories[memory.id] = memory

            // 更新索引
            updateIndices(memory)

            return true
        }
    }

    /**
     * 更新记忆
     *
     * @param memory 语义记忆
     * @return 是否成功更新
     */
    fun updateMemory(memory: SemanticMemory): Boolean {
        lock.write {
            // 检查记忆是否存在
            val existingMemory = memories[memory.id] ?: return false

            // 移除旧索引
            removeFromIndices(existingMemory)

            // 更新记忆
            memories[memory.id] = memory

            // 更新索引
            updateIndices(memory)

            return true
        }
    }

    /**
     * 获取记忆
     *
     * @param id 记忆 ID
     * @return 语义记忆，如果不存在则返回 null
     */
    fun getMemory(id: String): SemanticMemory? {
        lock.read {
            val memory = memories[id]
            memory?.access()
            return memory
        }
    }

    /**
     * 移除记忆
     *
     * @param id 记忆 ID
     * @return 是否成功移除
     */
    fun removeMemory(id: String): Boolean {
        lock.write {
            // 检查记忆是否存在
            val memory = memories[id] ?: return false

            // 移除索引
            removeFromIndices(memory)

            // 移除记忆
            memories.remove(id)

            // 移除相关记忆中的引用
            memory.relatedMemories.forEach { relatedId ->
                memories[relatedId]?.removeRelatedMemory(id)
            }

            return true
        }
    }

    /**
     * 获取所有记忆
     *
     * @return 记忆列表
     */
    fun getAllMemories(): List<SemanticMemory> {
        lock.read {
            return memories.values.toList()
        }
    }

    /**
     * 根据类型查找记忆
     *
     * @param type 记忆类型
     * @return 记忆列表
     */
    fun findMemoriesByType(type: MemoryType): List<SemanticMemory> {
        lock.read {
            val memoryIds = typeIndex[type] ?: return emptyList()

            return memoryIds.mapNotNull { memories[it] }
        }
    }

    /**
     * 根据重要性查找记忆
     *
     * @param importance 重要性级别
     * @return 记忆列表
     */
    fun findMemoriesByImportance(importance: ImportanceLevel): List<SemanticMemory> {
        lock.read {
            val memoryIds = importanceIndex[importance] ?: return emptyList()

            return memoryIds.mapNotNull { memories[it] }
        }
    }

    /**
     * 根据代码元素查找记忆
     *
     * @param elementId 代码元素 ID
     * @return 记忆列表
     */
    fun findMemoriesByElement(elementId: String): List<SemanticMemory> {
        lock.read {
            val memoryIds = elementIndex[elementId] ?: return emptyList()

            return memoryIds.mapNotNull { memories[it] }
        }
    }

    /**
     * 根据符号查找记忆
     *
     * @param symbolId 符号 ID
     * @return 记忆列表
     */
    fun findMemoriesBySymbol(symbolId: String): List<SemanticMemory> {
        lock.read {
            val memoryIds = symbolIndex[symbolId] ?: return emptyList()

            return memoryIds.mapNotNull { memories[it] }
        }
    }

    /**
     * 根据文件路径查找记忆
     *
     * @param filePath 文件路径
     * @return 记忆列表
     */
    fun findMemoriesByFile(filePath: Path): List<SemanticMemory> {
        lock.read {
            val memoryIds = filePathIndex[filePath.toString()] ?: return emptyList()

            return memoryIds.mapNotNull { memories[it] }
        }
    }

    /**
     * 查找相关记忆
     *
     * @param memoryId 记忆 ID
     * @return 记忆列表
     */
    fun findRelatedMemories(memoryId: String): List<SemanticMemory> {
        lock.read {
            val memory = memories[memoryId] ?: return emptyList()

            return memory.relatedMemories.mapNotNull { memories[it] }
        }
    }

    /**
     * 添加语义关系
     *
     * @param relationship 语义关系
     * @return 是否成功添加
     */
    fun addRelationship(relationship: SemanticRelationship): Boolean {
        lock.write {
            // 检查源记忆和目标记忆是否存在
            val sourceMemory = memories[relationship.sourceId] ?: return false
            val targetMemory = memories[relationship.targetId] ?: return false

            // 添加到关系索引
            relationshipIndex.computeIfAbsent(relationship.sourceId) { mutableSetOf() }.add(relationship)

            // 如果是双向关系，也添加到目标记忆的关系索引
            if (relationship.bidirectional) {
                relationshipIndex.computeIfAbsent(relationship.targetId) { mutableSetOf() }.add(relationship)
            }

            // 添加到关系类型索引
            relationshipTypeIndex.computeIfAbsent(relationship.type) { mutableSetOf() }.add(relationship.id)

            // 更新相关记忆
            sourceMemory.addRelatedMemory(relationship.targetId)
            if (relationship.bidirectional) {
                targetMemory.addRelatedMemory(relationship.sourceId)
            }

            return true
        }
    }

    /**
     * 获取记忆的语义关系
     *
     * @param memoryId 记忆 ID
     * @return 语义关系列表
     */
    fun getRelationships(memoryId: String): List<SemanticRelationship> {
        lock.read {
            return relationshipIndex[memoryId]?.toList() ?: emptyList()
        }
    }

    /**
     * 根据关系类型查找关系
     *
     * @param type 关系类型
     * @return 语义关系列表
     */
    fun findRelationshipsByType(type: SemanticRelationshipType): List<SemanticRelationship> {
        lock.read {
            val relationshipIds = relationshipTypeIndex[type] ?: return emptyList()

            return relationshipIds.flatMap { id ->
                relationshipIndex.values.flatMap { relationships ->
                    relationships.filter { it.id == id }
                }
            }
        }
    }

    /**
     * 查找引用记忆的记忆
     *
     * @param memoryId 记忆 ID
     * @return 记忆列表
     */
    fun findReferencingMemories(memoryId: String): List<SemanticMemory> {
        lock.read {
            val memoryIds = relatedMemoriesIndex[memoryId] ?: return emptyList()

            return memoryIds.mapNotNull { memories[it] }
        }
    }

    /**
     * 获取记忆数量
     *
     * @return 记忆数量
     */
    fun getMemoryCount(): Int {
        lock.read {
            return memories.size
        }
    }

    /**
     * 清空存储
     */
    fun clear() {
        lock.write {
            memories.clear()
            typeIndex.clear()
            importanceIndex.clear()
            elementIndex.clear()
            symbolIndex.clear()
            filePathIndex.clear()
            relatedMemoriesIndex.clear()
            relationshipIndex.clear()
            relationshipTypeIndex.clear()
        }
    }

    /**
     * 更新索引
     *
     * @param memory 语义记忆
     */
    private fun updateIndices(memory: SemanticMemory) {
        // 更新类型索引
        typeIndex.computeIfAbsent(memory.type) { mutableSetOf() }.add(memory.id)

        // 更新重要性索引
        importanceIndex.computeIfAbsent(memory.importance) { mutableSetOf() }.add(memory.id)

        // 更新代码元素索引
        memory.sourceElements.forEach { element ->
            elementIndex.computeIfAbsent(element.id) { mutableSetOf() }.add(memory.id)

            // 更新文件路径索引
            val filePath = element.location.filePath.toString()
            filePathIndex.computeIfAbsent(filePath) { mutableSetOf() }.add(memory.id)
        }

        // 更新符号索引
        memory.sourceSymbols.forEach { symbol ->
            symbolIndex.computeIfAbsent(symbol.id) { mutableSetOf() }.add(memory.id)
        }

        // 更新相关记忆索引
        memory.relatedMemories.forEach { relatedId ->
            relatedMemoriesIndex.computeIfAbsent(relatedId) { mutableSetOf() }.add(memory.id)
        }
    }

    /**
     * 从索引中移除
     *
     * @param memory 语义记忆
     */
    private fun removeFromIndices(memory: SemanticMemory) {
        // 移除类型索引
        typeIndex[memory.type]?.remove(memory.id)

        // 移除重要性索引
        importanceIndex[memory.importance]?.remove(memory.id)

        // 移除代码元素索引
        memory.sourceElements.forEach { element ->
            elementIndex[element.id]?.remove(memory.id)

            // 移除文件路径索引
            val filePath = element.location.filePath.toString()
            filePathIndex[filePath]?.remove(memory.id)
        }

        // 移除符号索引
        memory.sourceSymbols.forEach { symbol ->
            symbolIndex[symbol.id]?.remove(memory.id)
        }

        // 移除相关记忆索引
        memory.relatedMemories.forEach { relatedId ->
            relatedMemoriesIndex[relatedId]?.remove(memory.id)
        }
    }

    /**
     * 获取存储统计信息
     *
     * @return 统计信息
     */
    fun getStats(): Map<String, Any> {
        lock.read {
            val stats = mutableMapOf<String, Any>()

            stats["memoryCount"] = memories.size

            // 按类型统计记忆
            val memoriesByType = mutableMapOf<MemoryType, Int>()
            typeIndex.forEach { (type, ids) ->
                memoriesByType[type] = ids.size
            }
            stats["memoriesByType"] = memoriesByType

            // 按重要性统计记忆
            val memoriesByImportance = mutableMapOf<ImportanceLevel, Int>()
            importanceIndex.forEach { (importance, ids) ->
                memoriesByImportance[importance] = ids.size
            }
            stats["memoriesByImportance"] = memoriesByImportance

            // 统计索引大小
            stats["elementIndexSize"] = elementIndex.size
            stats["symbolIndexSize"] = symbolIndex.size
            stats["filePathIndexSize"] = filePathIndex.size
            stats["relatedMemoriesIndexSize"] = relatedMemoriesIndex.size
            stats["relationshipIndexSize"] = relationshipIndex.size
            stats["relationshipTypeIndexSize"] = relationshipTypeIndex.size

            return stats
        }
    }
}
