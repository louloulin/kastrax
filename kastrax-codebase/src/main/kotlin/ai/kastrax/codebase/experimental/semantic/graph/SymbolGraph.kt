package ai.kastrax.codebase.semantic.graph

import ai.kastrax.codebase.semantic.model.CodeElement
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 符号关系类型
 */
enum class SymbolRelationType {
    /**
     * 定义关系
     */
    DEFINES,
    
    /**
     * 引用关系
     */
    REFERENCES,
    
    /**
     * 继承关系
     */
    EXTENDS,
    
    /**
     * 实现关系
     */
    IMPLEMENTS,
    
    /**
     * 调用关系
     */
    CALLS,
    
    /**
     * 包含关系
     */
    CONTAINS,
    
    /**
     * 导入关系
     */
    IMPORTS,
    
    /**
     * 重写关系
     */
    OVERRIDES,
    
    /**
     * 使用关系
     */
    USES,
    
    /**
     * 依赖关系
     */
    DEPENDS_ON
}

/**
 * 符号关系
 *
 * @property sourceId 源符号ID
 * @property targetId 目标符号ID
 * @property type 关系类型
 * @property metadata 元数据
 */
data class SymbolRelation(
    val sourceId: String,
    val targetId: String,
    val type: SymbolRelationType,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * 符号图
 *
 * 表示代码中符号之间的关系
 */
class SymbolGraph {
    // 符号映射表（ID -> 代码元素）
    private val symbols = ConcurrentHashMap<String, CodeElement>()
    
    // 符号关系列表
    private val relations = ConcurrentHashMap<String, MutableList<SymbolRelation>>()
    
    // 文件映射表（文件路径 -> 符号ID列表）
    private val fileSymbols = ConcurrentHashMap<Path, MutableList<String>>()
    
    // 符号类型映射表（类型 -> 符号ID列表）
    private val typeSymbols = ConcurrentHashMap<String, MutableList<String>>()
    
    /**
     * 添加符号
     *
     * @param element 代码元素
     * @return 是否成功添加
     */
    fun addSymbol(element: CodeElement): Boolean {
        val id = element.id
        
        if (symbols.containsKey(id)) {
            return false
        }
        
        symbols[id] = element
        
        // 更新文件映射表
        val filePath = element.location.filePath
        fileSymbols.computeIfAbsent(filePath) { mutableListOf() }.add(id)
        
        // 更新类型映射表
        val type = element.type.name
        typeSymbols.computeIfAbsent(type) { mutableListOf() }.add(id)
        
        return true
    }
    
    /**
     * 添加关系
     *
     * @param sourceId 源符号ID
     * @param targetId 目标符号ID
     * @param type 关系类型
     * @param metadata 元数据
     * @return 是否成功添加
     */
    fun addRelation(
        sourceId: String,
        targetId: String,
        type: SymbolRelationType,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        if (!symbols.containsKey(sourceId) || !symbols.containsKey(targetId)) {
            return false
        }
        
        val relation = SymbolRelation(
            sourceId = sourceId,
            targetId = targetId,
            type = type,
            metadata = metadata.toMutableMap()
        )
        
        relations.computeIfAbsent(sourceId) { mutableListOf() }.add(relation)
        
        return true
    }
    
    /**
     * 添加关系
     *
     * @param source 源符号
     * @param target 目标符号
     * @param type 关系类型
     * @param metadata 元数据
     * @return 是否成功添加
     */
    fun addRelation(
        source: CodeElement,
        target: CodeElement,
        type: SymbolRelationType,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        return addRelation(source.id, target.id, type, metadata)
    }
    
    /**
     * 获取符号
     *
     * @param id 符号ID
     * @return 代码元素，如果不存在则返回 null
     */
    fun getSymbol(id: String): CodeElement? {
        return symbols[id]
    }
    
    /**
     * 获取所有符号
     *
     * @return 所有符号的集合
     */
    fun getAllSymbols(): Collection<CodeElement> {
        return symbols.values
    }
    
    /**
     * 获取符号的出边关系
     *
     * @param id 符号ID
     * @return 出边关系列表
     */
    fun getOutgoingRelations(id: String): List<SymbolRelation> {
        return relations[id] ?: emptyList()
    }
    
    /**
     * 获取符号的入边关系
     *
     * @param id 符号ID
     * @return 入边关系列表
     */
    fun getIncomingRelations(id: String): List<SymbolRelation> {
        val result = mutableListOf<SymbolRelation>()
        
        relations.values.forEach { relationList ->
            relationList.filter { it.targetId == id }.forEach { relation ->
                result.add(relation)
            }
        }
        
        return result
    }
    
    /**
     * 获取文件中的所有符号
     *
     * @param filePath 文件路径
     * @return 符号列表
     */
    fun getFileSymbols(filePath: Path): List<CodeElement> {
        val symbolIds = fileSymbols[filePath] ?: return emptyList()
        return symbolIds.mapNotNull { symbols[it] }
    }
    
    /**
     * 获取指定类型的所有符号
     *
     * @param type 符号类型
     * @return 符号列表
     */
    fun getSymbolsByType(type: String): List<CodeElement> {
        val symbolIds = typeSymbols[type] ?: return emptyList()
        return symbolIds.mapNotNull { symbols[it] }
    }
    
    /**
     * 删除文件中的所有符号
     *
     * @param filePath 文件路径
     * @return 删除的符号数量
     */
    fun removeFileSymbols(filePath: Path): Int {
        val symbolIds = fileSymbols[filePath] ?: return 0
        var count = 0
        
        symbolIds.forEach { id ->
            if (symbols.remove(id) != null) {
                count++
            }
            
            // 删除相关的关系
            relations.remove(id)
            relations.values.forEach { relationList ->
                relationList.removeIf { it.targetId == id }
            }
        }
        
        fileSymbols.remove(filePath)
        
        // 更新类型映射表
        typeSymbols.values.forEach { idList ->
            idList.removeIf { it in symbolIds }
        }
        
        return count
    }
    
    /**
     * 查找符号
     *
     * @param predicate 谓词函数
     * @return 符合条件的符号列表
     */
    fun findSymbols(predicate: (CodeElement) -> Boolean): List<CodeElement> {
        return symbols.values.filter(predicate)
    }
    
    /**
     * 查找关系
     *
     * @param predicate 谓词函数
     * @return 符合条件的关系列表
     */
    fun findRelations(predicate: (SymbolRelation) -> Boolean): List<SymbolRelation> {
        val result = mutableListOf<SymbolRelation>()
        
        relations.values.forEach { relationList ->
            relationList.filter(predicate).forEach { relation ->
                result.add(relation)
            }
        }
        
        return result
    }
    
    /**
     * 获取符号的所有引用
     *
     * @param id 符号ID
     * @return 引用该符号的符号列表
     */
    fun getReferences(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.REFERENCES }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有定义
     *
     * @param id 符号ID
     * @return 定义该符号的符号列表
     */
    fun getDefinitions(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.DEFINES }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有调用
     *
     * @param id 符号ID
     * @return 调用该符号的符号列表
     */
    fun getCalls(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.CALLS }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有实现
     *
     * @param id 符号ID
     * @return 实现该符号的符号列表
     */
    fun getImplementations(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.IMPLEMENTS }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有子类
     *
     * @param id 符号ID
     * @return 继承该符号的符号列表
     */
    fun getSubclasses(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.EXTENDS }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有重写
     *
     * @param id 符号ID
     * @return 重写该符号的符号列表
     */
    fun getOverrides(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.OVERRIDES }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有使用
     *
     * @param id 符号ID
     * @return 使用该符号的符号列表
     */
    fun getUses(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.USES }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有依赖
     *
     * @param id 符号ID
     * @return 依赖该符号的符号列表
     */
    fun getDependents(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.DEPENDS_ON }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有包含
     *
     * @param id 符号ID
     * @return 包含该符号的符号列表
     */
    fun getContainers(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.CONTAINS }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有导入
     *
     * @param id 符号ID
     * @return 导入该符号的符号列表
     */
    fun getImporters(id: String): List<CodeElement> {
        val relations = getIncomingRelations(id).filter { it.type == SymbolRelationType.IMPORTS }
        return relations.mapNotNull { symbols[it.sourceId] }
    }
    
    /**
     * 获取符号的所有关系
     *
     * @param id 符号ID
     * @return 与该符号相关的所有关系
     */
    fun getAllRelations(id: String): List<SymbolRelation> {
        val outgoing = getOutgoingRelations(id)
        val incoming = getIncomingRelations(id)
        return outgoing + incoming
    }
    
    /**
     * 获取符号的所有相关符号
     *
     * @param id 符号ID
     * @return 与该符号相关的所有符号
     */
    fun getAllRelatedSymbols(id: String): List<CodeElement> {
        val relations = getAllRelations(id)
        val relatedIds = relations.flatMap { listOf(it.sourceId, it.targetId) }.distinct().filter { it != id }
        return relatedIds.mapNotNull { symbols[it] }
    }
    
    /**
     * 清空图
     */
    fun clear() {
        symbols.clear()
        relations.clear()
        fileSymbols.clear()
        typeSymbols.clear()
    }
    
    /**
     * 获取符号数量
     *
     * @return 符号数量
     */
    fun getSymbolCount(): Int {
        return symbols.size
    }
    
    /**
     * 获取关系数量
     *
     * @return 关系数量
     */
    fun getRelationCount(): Int {
        return relations.values.sumOf { it.size }
    }
    
    /**
     * 获取文件数量
     *
     * @return 文件数量
     */
    fun getFileCount(): Int {
        return fileSymbols.size
    }
    
    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    fun getStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        stats["symbolCount"] = getSymbolCount()
        stats["relationCount"] = getRelationCount()
        stats["fileCount"] = getFileCount()
        
        val symbolTypeCount = mutableMapOf<String, Int>()
        typeSymbols.forEach { (type, ids) ->
            symbolTypeCount[type] = ids.size
        }
        stats["symbolTypeCount"] = symbolTypeCount
        
        val relationTypeCount = mutableMapOf<String, Int>()
        relations.values.forEach { relationList ->
            relationList.forEach { relation ->
                val type = relation.type.name
                relationTypeCount[type] = relationTypeCount.getOrDefault(type, 0) + 1
            }
        }
        stats["relationTypeCount"] = relationTypeCount
        
        return stats
    }
}
