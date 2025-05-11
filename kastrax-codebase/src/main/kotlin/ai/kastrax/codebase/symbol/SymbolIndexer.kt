package ai.kastrax.codebase.symbol

import ai.kastrax.codebase.symbol.model.SymbolGraph
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * 符号索引器配置
 *
 * @property enableFullTextSearch 是否启用全文搜索
 * @property enableFuzzySearch 是否启用模糊搜索
 * @property maxSearchResults 最大搜索结果数
 * @property minScoreThreshold 最小分数阈值
 */
data class SymbolIndexerConfig(
    val enableFullTextSearch: Boolean = true,
    val enableFuzzySearch: Boolean = true,
    val maxSearchResults: Int = 100,
    val minScoreThreshold: Double = 0.5
)

/**
 * 符号索引器
 *
 * 用于索引和查询符号信息
 *
 * @property config 配置
 */
class SymbolIndexer(
    private val config: SymbolIndexerConfig = SymbolIndexerConfig()
) {
    // 符号图
    private var symbolGraph: SymbolGraph? = null
    
    // 名称索引
    private val nameIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 限定名索引
    private val qualifiedNameIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 文件路径索引
    private val filePathIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 类型索引
    private val typeIndex = ConcurrentHashMap<SymbolType, MutableSet<String>>()
    
    // 全文索引
    private val fullTextIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    // 读写锁
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 索引符号图
     *
     * @param graph 符号图
     */
    suspend fun indexGraph(graph: SymbolGraph) = withContext(Dispatchers.Default) {
        lock.write {
            logger.info { "开始索引符号图: ${graph.name}" }
            
            // 保存符号图
            symbolGraph = graph
            
            // 清除索引
            clearIndices()
            
            // 索引所有节点
            val nodes = graph.getAllNodes()
            nodes.forEach { node ->
                indexNode(node)
            }
            
            logger.info { "符号图索引完成，共索引 ${nodes.size} 个符号" }
        }
    }
    
    /**
     * 索引符号节点
     *
     * @param node 符号节点
     */
    private fun indexNode(node: SymbolNode) {
        // 索引名称
        nameIndex.computeIfAbsent(node.name.lowercase()) { mutableSetOf() }.add(node.id)
        
        // 索引限定名
        qualifiedNameIndex.computeIfAbsent(node.qualifiedName.lowercase()) { mutableSetOf() }.add(node.id)
        
        // 索引文件路径
        val filePath = node.location.filePath.toString()
        filePathIndex.computeIfAbsent(filePath) { mutableSetOf() }.add(node.id)
        
        // 索引类型
        typeIndex.computeIfAbsent(node.type) { mutableSetOf() }.add(node.id)
        
        // 全文索引
        if (config.enableFullTextSearch) {
            // 索引名称的每个部分
            node.name.split(Regex("[^a-zA-Z0-9]")).forEach { part ->
                if (part.isNotEmpty()) {
                    fullTextIndex.computeIfAbsent(part.lowercase()) { mutableSetOf() }.add(node.id)
                }
            }
            
            // 索引限定名的每个部分
            node.qualifiedName.split(Regex("[^a-zA-Z0-9]")).forEach { part ->
                if (part.isNotEmpty()) {
                    fullTextIndex.computeIfAbsent(part.lowercase()) { mutableSetOf() }.add(node.id)
                }
            }
            
            // 索引文档注释
            node.codeElement?.documentation?.split(Regex("\\s+"))?.forEach { word ->
                if (word.length > 3) {
                    fullTextIndex.computeIfAbsent(word.lowercase()) { mutableSetOf() }.add(node.id)
                }
            }
        }
    }
    
    /**
     * 清除索引
     */
    private fun clearIndices() {
        nameIndex.clear()
        qualifiedNameIndex.clear()
        filePathIndex.clear()
        typeIndex.clear()
        fullTextIndex.clear()
    }
    
    /**
     * 根据名称查找符号
     *
     * @param name 符号名称
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun findSymbolsByName(name: String, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val graph = symbolGraph ?: return emptyList()
            
            // 查找名称索引
            val nodeIds = nameIndex[name.lowercase()] ?: return emptyList()
            
            // 获取符号节点
            return nodeIds.mapNotNull { graph.getNode(it) }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 根据限定名查找符号
     *
     * @param qualifiedName 限定名
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun findSymbolsByQualifiedName(qualifiedName: String, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val graph = symbolGraph ?: return emptyList()
            
            // 查找限定名索引
            val nodeIds = qualifiedNameIndex[qualifiedName.lowercase()] ?: return emptyList()
            
            // 获取符号节点
            return nodeIds.mapNotNull { graph.getNode(it) }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 根据文件路径查找符号
     *
     * @param filePath 文件路径
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun findSymbolsByFile(filePath: Path, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val graph = symbolGraph ?: return emptyList()
            
            // 查找文件路径索引
            val nodeIds = filePathIndex[filePath.toString()] ?: return emptyList()
            
            // 获取符号节点
            return nodeIds.mapNotNull { graph.getNode(it) }
                .filter { type == null || it.type == type }
        }
    }
    
    /**
     * 根据类型查找符号
     *
     * @param type 符号类型
     * @return 符号节点列表
     */
    fun findSymbolsByType(type: SymbolType): List<SymbolNode> {
        lock.read {
            val graph = symbolGraph ?: return emptyList()
            
            // 查找类型索引
            val nodeIds = typeIndex[type] ?: return emptyList()
            
            // 获取符号节点
            return nodeIds.mapNotNull { graph.getNode(it) }
        }
    }
    
    /**
     * 全文搜索符号
     *
     * @param query 查询字符串
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun searchSymbols(query: String, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val graph = symbolGraph ?: return emptyList()
            
            if (!config.enableFullTextSearch) {
                return findSymbolsByName(query, type)
            }
            
            // 分词
            val terms = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
            
            if (terms.isEmpty()) {
                return emptyList()
            }
            
            // 查找每个词的结果
            val resultSets = terms.map { term ->
                val exactMatches = nameIndex[term.lowercase()] ?: emptySet()
                val prefixMatches = if (config.enableFuzzySearch) {
                    fullTextIndex.entries
                        .filter { it.key.startsWith(term.lowercase()) }
                        .flatMap { it.value }
                        .toSet()
                } else {
                    emptySet()
                }
                
                exactMatches + prefixMatches
            }
            
            // 取交集
            val intersection = resultSets.reduce { acc, set -> acc.intersect(set) }
            
            // 获取符号节点
            return intersection.mapNotNull { graph.getNode(it) }
                .filter { type == null || it.type == type }
                .take(config.maxSearchResults)
        }
    }
    
    /**
     * 模糊搜索符号
     *
     * @param query 查询字符串
     * @param type 符号类型，如果为 null 则不限制类型
     * @return 符号节点列表
     */
    fun fuzzySearchSymbols(query: String, type: SymbolType? = null): List<SymbolNode> {
        lock.read {
            val graph = symbolGraph ?: return emptyList()
            
            if (!config.enableFuzzySearch) {
                return findSymbolsByName(query, type)
            }
            
            // 查找前缀匹配
            val prefixMatches = nameIndex.entries
                .filter { it.key.startsWith(query.lowercase()) }
                .flatMap { it.value }
                .toSet()
            
            // 查找包含匹配
            val containsMatches = if (query.length >= 3) {
                nameIndex.entries
                    .filter { it.key.contains(query.lowercase()) && !it.key.startsWith(query.lowercase()) }
                    .flatMap { it.value }
                    .toSet()
            } else {
                emptySet()
            }
            
            // 合并结果
            val allMatches = prefixMatches + containsMatches
            
            // 获取符号节点
            return allMatches.mapNotNull { graph.getNode(it) }
                .filter { type == null || it.type == type }
                .sortedBy { it.name.length } // 优先返回名称较短的符号
                .take(config.maxSearchResults)
        }
    }
    
    /**
     * 获取符号图
     *
     * @return 符号图
     */
    fun getGraph(): SymbolGraph? {
        lock.read {
            return symbolGraph
        }
    }
    
    /**
     * 获取索引统计信息
     *
     * @return 统计信息
     */
    fun getStats(): Map<String, Any> {
        lock.read {
            val stats = mutableMapOf<String, Any>()
            
            stats["nameIndexSize"] = nameIndex.size
            stats["qualifiedNameIndexSize"] = qualifiedNameIndex.size
            stats["filePathIndexSize"] = filePathIndex.size
            stats["typeIndexSize"] = typeIndex.size
            stats["fullTextIndexSize"] = fullTextIndex.size
            
            val graph = symbolGraph
            if (graph != null) {
                stats["graphStats"] = graph.getStats()
            }
            
            return stats
        }
    }
}
