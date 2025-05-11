
package ai.kastrax.codebase.symbol.model

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 符号关系图
 *
 * 代表代码库中符号之间的关系图
 *
 * @property name 图名称
 */
class SymbolGraph(
    val name: String
) {
    // 符号节点映射
    private val nodes = ConcurrentHashMap<String, SymbolNode>()
    
    // 符号关系映射
    private val relations = ConcurrentHashMap<String, SymbolRelation>()
    
    // 源符号到关系的映射
    private val sourceToRelations = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 目标符号到关系的映射
    private val targetToRelations = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 符号名称到符号 ID 的映射
    private val nameToIds = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 限定名到符号 ID 的映射
    private val qualifiedNameToIds = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 文件路径到符号 ID 的映射
    private val fileToIds = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 符号类型到符号 ID 的映射
    private val typeToIds = ConcurrentHashMap<SymbolType, MutableSet<String>>()
    
    // 读写锁
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 添加符号节点
     *
     * @param node 符号节点
     * @return 是否成功添加
     */
    fun addNode(node: SymbolNode): Boolean {
        lock.write {
            // 检查节点是否已存在
            if (nodes.containsKey(node.id)) {
                return false
            }
            
            // 添加节点
            nodes[node.id] = node
            
            // 更新索引
            nameToIds.computeIfAbsent(node.name) { mutableSetOf() }.add(node.id)
            qualifiedNameToIds.computeIfAbsent(node.qualifiedName) { mutableSetOf() }.add(node.id)
            typeToIds.computeIfAbsent(node.type) { mutableSetOf() }.add(node.id)
            
            // 更新文件索引
            val filePath = node.location.filePath.toString()
            fileToIds.computeIfAbsent(filePath) { mutableSetOf() }.add(node.id)
            
            return true
        }
    }
    
    /**
     * 添加符号关系
     *
     * @param relation 符号关系
     * @return 是否成功添加
     */
    fun addRelation(relation: SymbolRelation): Boolean {
        lock.write {
            // 检查关系是否已存在
            if (relations.containsKey(relation.id)) {
                return false
            }
            
            // 检查源节点和目标节点是否存在
            if (!nodes.containsKey(relation.sourceId) || !nodes.containsKey(relation.targetId)) {
                return false
            }
            
            // 添加关系
            relations[relation.id] = relation
            
            // 更新索引
            sourceToRelations.computeIfAbsent(relation.sourceId) { mutableSetOf() }.add(relation.id)
            targetToRelations.computeIfAbsent(relation.targetId) { mutableSetOf() }.add(relation.id)
            
            return true
        }
    }
    
    /**
     * 获取符号节点
     *
     * @param id 符号 ID
     * @return 符号节点，如果不存在则返回 null
     */
    fun getNode(id: String): SymbolNode? {
        lock.read {
            return nodes[id]
        }
    }
    
    /**
     * 获取符号关系
     *
     * @param id 关系 ID
     * @return 符号关系，如果不存在则返回 null
     */
    fun getRelation(id: String): SymbolRelation? {
        lock.read {
            return relations[id]
        }
    }
    
    /**
     * 获取所有符号节点
     *
     * @return 符号节点列表
     */
    fun getAllNodes(): List<SymbolNode> {
        lock.read {
            return nodes.values.toList()
        }
    }
    
    /**
     * 获取所有符号关系
     *
     * @return 符号关系列表
     */
    fun getAllRelations(): List<SymbolRelation> {
        lock.read {
            return relations.values.toList()
        }
    }
    
    /**
     * 获取出关系
     *
     * @param nodeId 节点 ID
     * @param type 关系类型，如果为 null 则不限制类型
     * @return 符号关系列表
     */
    fun getOutgoingRelations(nodeId: String, type: SymbolRelationType? = null): List<SymbolRelation> {
        lock.read {
            val relationIds = sourceToRelations[nodeId] ?: return emptyList()
            
            return relationIds.mapNotNull { relations[it] }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 获取入关系
     *
     * @param nodeId 节点 ID
     * @param type 关系类型，如果为 null 则不限制类型
     * @return 符号关系列表
     */
    fun getIncomingRelations(nodeId: String, type: SymbolRelationType? = null): List<SymbolRelation> {
        lock.read {
            val relationIds = targetToRelations[nodeId] ?: return emptyList()
            
            return relationIds.mapNotNull { relations[it] }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 获取相邻节点
     *
     * @param nodeId 节点 ID
     * @param direction 方向，true 表示出方向，false 表示入方向
     * @param type 关系类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun getAdjacentNodes(nodeId: String, direction: Boolean = true, type: SymbolRelationType? = null): List<SymbolNode> {
        lock.read {
            val relations = if (direction) {
                getOutgoingRelations(nodeId, type)
            } else {
                getIncomingRelations(nodeId, type)
            }
            
            return relations.mapNotNull { relation ->
                val targetId = if (direction) relation.targetId else relation.sourceId
                nodes[targetId]
            }
        }
    }
    
    /**
     * 根据名称查找符号节点
     *
     * @param name 符号名称
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun findNodesByName(name: String, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val nodeIds = nameToIds[name] ?: return emptyList()
            
            return nodeIds.mapNotNull { nodes[it] }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 根据限定名查找符号节点
     *
     * @param qualifiedName 限定名
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun findNodesByQualifiedName(qualifiedName: String, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val nodeIds = qualifiedNameToIds[qualifiedName] ?: return emptyList()
            
            return nodeIds.mapNotNull { nodes[it] }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 根据文件路径查找符号节点
     *
     * @param filePath 文件路径
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun findNodesByFile(filePath: Path, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val nodeIds = fileToIds[filePath.toString()] ?: return emptyList()
            
            return nodeIds.mapNotNull { nodes[it] }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 根据类型查找符号节点
     *
     * @param type 符号类型
     * @return 符号节点列表
     */
    fun findNodesByType(type: SymbolType): List<SymbolNode> {
        lock.read {
            val nodeIds = typeToIds[type] ?: return emptyList()
            
            return nodeIds.mapNotNull { nodes[it] }
        }
    }
    
    /**
     * 获取节点数量
     *
     * @return 节点数量
     */
    fun getNodeCount(): Int {
        lock.read {
            return nodes.size
        }
    }
    
    /**
     * 获取关系数量
     *
     * @return 关系数量
     */
    fun getRelationCount(): Int {
        lock.read {
            return relations.size
        }
    }
    
    /**
     * 清空图
     */
    fun clear() {
        lock.write {
            nodes.clear()
            relations.clear()
            sourceToRelations.clear()
            targetToRelations.clear()
            nameToIds.clear()
            qualifiedNameToIds.clear()
            fileToIds.clear()
            typeToIds.clear()
        }
    }
    
    /**
     * 移除节点
     *
     * @param nodeId 节点 ID
     * @return 是否成功移除
     */
    fun removeNode(nodeId: String): Boolean {
        lock.write {
            // 获取节点
            val node = nodes[nodeId] ?: return false
            
            // 移除关联的关系
            val outRelationIds = sourceToRelations[nodeId]?.toList() ?: emptyList()
            val inRelationIds = targetToRelations[nodeId]?.toList() ?: emptyList()
            
            outRelationIds.forEach { removeRelation(it) }
            inRelationIds.forEach { removeRelation(it) }
            
            // 移除节点
            nodes.remove(nodeId)
            
            // 更新索引
            nameToIds[node.name]?.remove(nodeId)
            qualifiedNameToIds[node.qualifiedName]?.remove(nodeId)
            typeToIds[node.type]?.remove(nodeId)
            fileToIds[node.location.filePath.toString()]?.remove(nodeId)
            
            return true
        }
    }
    
    /**
     * 移除关系
     *
     * @param relationId 关系 ID
     * @return 是否成功移除
     */
    fun removeRelation(relationId: String): Boolean {
        lock.write {
            // 获取关系
            val relation = relations[relationId] ?: return false
            
            // 移除关系
            relations.remove(relationId)
            
            // 更新索引
            sourceToRelations[relation.sourceId]?.remove(relationId)
            targetToRelations[relation.targetId]?.remove(relationId)
            
            return true
        }
    }
    
    /**
     * 获取图的统计信息
     *
     * @return 统计信息
     */
    fun getStats(): Map<String, Any> {
        lock.read {
            val stats = mutableMapOf<String, Any>()
            
            stats["nodeCount"] = nodes.size
            stats["relationCount"] = relations.size
            
            // 按类型统计节点
            val nodesByType = mutableMapOf<SymbolType, Int>()
            typeToIds.forEach { (type, ids) ->
                nodesByType[type] = ids.size
            }
            stats["nodesByType"] = nodesByType
            
            // 按类型统计关系
            val relationsByType = mutableMapOf<SymbolRelationType, Int>()
            relations.values.forEach { relation ->
                relationsByType[relation.type] = relationsByType.getOrDefault(relation.type, 0) + 1
            }
            stats["relationsByType"] = relationsByType
            
            return stats
        }
    }
}
