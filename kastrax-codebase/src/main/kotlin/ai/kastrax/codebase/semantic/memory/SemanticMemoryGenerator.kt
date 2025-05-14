package ai.kastrax.codebase.semantic.memory

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 语义记忆生成器配置
 *
 * @property generateCodeStructureMemories 是否生成代码结构记忆
 * @property generateSymbolDefinitionMemories 是否生成符号定义记忆
 * @property generateSymbolReferenceMemories 是否生成符号引用记忆
 * @property generateInheritanceMemories 是否生成继承记忆
 * @property generateImplementationMemories 是否生成实现记忆
 * @property generateCallHierarchyMemories 是否生成调用层次结构记忆
 * @property generateImportDependencyMemories 是否生成导入依赖记忆
 * @property generateLibraryUsageMemories 是否生成库使用记忆
 * @property generateSemanticRelationMemories 是否生成语义关系记忆
 * @property maxConcurrentTasks 最大并发任务数
 */
data class SemanticMemoryGeneratorConfig(
    val generateCodeStructureMemories: Boolean = true,
    val generateSymbolDefinitionMemories: Boolean = true,
    val generateSymbolReferenceMemories: Boolean = true,
    val generateInheritanceMemories: Boolean = true,
    val generateImplementationMemories: Boolean = true,
    val generateCallHierarchyMemories: Boolean = true,
    val generateImportDependencyMemories: Boolean = true,
    val generateLibraryUsageMemories: Boolean = true,
    val generateSemanticRelationMemories: Boolean = true,
    val maxConcurrentTasks: Int = 10
)

/**
 * 语义记忆生成器
 *
 * 用于从代码元素和符号节点生成语义记忆
 *
 * @property config 配置
 */
class SemanticMemoryGenerator(
    private val config: SemanticMemoryGeneratorConfig = SemanticMemoryGeneratorConfig()
) {
    /**
     * 从代码元素生成记忆
     *
     * @param element 代码元素
     * @return 语义记忆列表
     */
    suspend fun generateMemoriesFromCodeElement(element: CodeElement): List<SemanticMemory> = withContext(Dispatchers.Default) {
        val memories = mutableListOf<SemanticMemory>()

        // 生成代码结构记忆
        if (config.generateCodeStructureMemories) {
            val structureMemory = generateCodeStructureMemory(element)
            if (structureMemory != null) {
                memories.add(structureMemory)
            }
        }

        // 生成符号定义记忆
        if (config.generateSymbolDefinitionMemories) {
            val definitionMemory = generateSymbolDefinitionMemory(element)
            if (definitionMemory != null) {
                memories.add(definitionMemory)
            }
        }

        // 递归处理子元素
        val childMemories = coroutineScope {
            element.children.chunked(config.maxConcurrentTasks).flatMap { chunk ->
                chunk.map { child ->
                    async {
                        generateMemoriesFromCodeElement(child)
                    }
                }.awaitAll().flatten()
            }
        }

        memories.addAll(childMemories)

        return@withContext memories
    }

    /**
     * 从符号节点生成记忆
     *
     * @param node 符号节点
     * @param graph 符号图
     * @return 语义记忆列表
     */
    suspend fun generateMemoriesFromSymbolNode(
        node: SymbolNode,
        graph: ai.kastrax.codebase.symbol.model.SymbolGraph
    ): List<SemanticMemory> = withContext(Dispatchers.Default) {
        val memories = mutableListOf<SemanticMemory>()

        // 生成符号定义记忆
        if (config.generateSymbolDefinitionMemories) {
            val definitionMemory = generateSymbolDefinitionMemory(node)
            if (definitionMemory != null) {
                memories.add(definitionMemory)
            }
        }

        // 生成符号引用记忆
        if (config.generateSymbolReferenceMemories) {
            val referenceMemories = generateSymbolReferenceMemories(node, graph)
            memories.addAll(referenceMemories)
        }

        // 生成继承记忆
        if (config.generateInheritanceMemories) {
            val inheritanceMemories = generateInheritanceMemories(node, graph)
            memories.addAll(inheritanceMemories)
        }

        // 生成实现记忆
        if (config.generateImplementationMemories) {
            val implementationMemories = generateImplementationMemories(node, graph)
            memories.addAll(implementationMemories)
        }

        // 生成调用层次结构记忆
        if (config.generateCallHierarchyMemories) {
            val callHierarchyMemories = generateCallHierarchyMemories(node, graph)
            memories.addAll(callHierarchyMemories)
        }

        // 生成导入依赖记忆
        if (config.generateImportDependencyMemories) {
            val importDependencyMemories = generateImportDependencyMemories(node, graph)
            memories.addAll(importDependencyMemories)
        }

        return@withContext memories
    }

    /**
     * 生成代码结构记忆
     *
     * @param element 代码元素
     * @return 语义记忆
     */
    private fun generateCodeStructureMemory(element: CodeElement): SemanticMemory? {
        // 只为特定类型的元素生成结构记忆
        if (element.type !in setOf(
                CodeElementType.FILE,
                CodeElementType.PACKAGE,
                CodeElementType.CLASS,
                CodeElementType.INTERFACE,
                CodeElementType.ENUM,
                CodeElementType.ANNOTATION
            )) {
            return null
        }

        val content = buildString {
            append("${element.type.name.lowercase()} ${element.name}")

            if (element.children.isNotEmpty()) {
                append(" 包含 ${element.children.size} 个子元素：")

                // 按类型分组子元素
                val childrenByType = element.children.groupBy { it.type }

                childrenByType.forEach { (type, children) ->
                    append("\n- ${children.size} 个 ${type.name.lowercase()}")

                    // 列出前几个子元素
                    children.take(5).forEach { child ->
                        append("\n  - ${child.name}")
                    }

                    if (children.size > 5) {
                        append("\n  - ... 等 ${children.size - 5} 个")
                    }
                }
            }
        }

        return SemanticMemory(
            type = MemoryType.CODE_STRUCTURE,
            content = content,
            sourceElements = listOf(element),
            importance = when (element.type) {
                CodeElementType.FILE, CodeElementType.PACKAGE -> ImportanceLevel.MEDIUM
                CodeElementType.CLASS, CodeElementType.INTERFACE -> ImportanceLevel.HIGH
                else -> ImportanceLevel.MEDIUM
            }
        )
    }

    /**
     * 生成符号定义记忆
     *
     * @param element 代码元素
     * @return 语义记忆
     */
    private fun generateSymbolDefinitionMemory(element: CodeElement): SemanticMemory? {
        // 只为特定类型的元素生成定义记忆
        if (element.type !in setOf(
                CodeElementType.CLASS,
                CodeElementType.INTERFACE,
                CodeElementType.ENUM,
                CodeElementType.ANNOTATION,
                CodeElementType.METHOD,
                CodeElementType.CONSTRUCTOR,
                CodeElementType.FIELD,
                CodeElementType.PROPERTY
            )) {
            return null
        }

        val content = buildString {
            append("${element.type.name.lowercase()} ${element.name} 定义在 ${element.location.filePath.substringAfterLast('/')}:${element.location.startLine}")

            // 添加可见性和修饰符
            val visibilityStr = if (element.visibility != ai.kastrax.codebase.semantic.model.Visibility.UNKNOWN) {
                element.visibility.name.lowercase()
            } else {
                ""
            }

            val modifiersStr = if (element.modifiers.isNotEmpty()) {
                element.modifiers.joinToString(" ") { it.name.lowercase() }
            } else {
                ""
            }

            if (visibilityStr.isNotEmpty() || modifiersStr.isNotEmpty()) {
                append("\n修饰符: $visibilityStr $modifiersStr".trim())
            }

            // 添加文档注释
            if (element.documentation.isNotEmpty()) {
                append("\n文档: ${element.documentation.take(200)}")
                if (element.documentation.length > 200) {
                    append("...")
                }
            }
        }

        return SemanticMemory(
            type = MemoryType.SYMBOL_DEFINITION,
            content = content,
            sourceElements = listOf(element),
            importance = when (element.type) {
                CodeElementType.CLASS, CodeElementType.INTERFACE -> ImportanceLevel.HIGH
                CodeElementType.METHOD, CodeElementType.CONSTRUCTOR -> ImportanceLevel.MEDIUM
                else -> ImportanceLevel.MEDIUM
            }
        )
    }

    /**
     * 生成符号定义记忆
     *
     * @param node 符号节点
     * @return 语义记忆
     */
    private fun generateSymbolDefinitionMemory(node: SymbolNode): SemanticMemory? {
        // 只为特定类型的符号生成定义记忆
        if (node.type !in setOf(
                SymbolType.CLASS,
                SymbolType.INTERFACE,
                SymbolType.ENUM,
                SymbolType.ANNOTATION,
                SymbolType.METHOD,
                SymbolType.CONSTRUCTOR,
                SymbolType.FIELD,
                SymbolType.PROPERTY
            )) {
            return null
        }

        val content = buildString {
            append("${node.type.name.lowercase()} ${node.name} 定义在 ${node.location.filePath.substringAfterLast('/')}:${node.location.startLine}")

            // 添加可见性
            val visibilityStr = if (node.visibility != ai.kastrax.codebase.semantic.model.Visibility.UNKNOWN) {
                node.visibility.name.lowercase()
            } else {
                ""
            }

            if (visibilityStr.isNotEmpty()) {
                append("\n可见性: $visibilityStr")
            }

            // 添加元数据
            if (node.metadata.isNotEmpty()) {
                append("\n元数据:")
                node.metadata.entries.take(5).forEach { (key, value) ->
                    append("\n- $key: $value")
                }

                if (node.metadata.size > 5) {
                    append("\n- ... 等 ${node.metadata.size - 5} 项")
                }
            }
        }

        return SemanticMemory(
            type = MemoryType.SYMBOL_DEFINITION,
            content = content,
            sourceSymbols = listOf(node),
            importance = when (node.type) {
                SymbolType.CLASS, SymbolType.INTERFACE -> ImportanceLevel.HIGH
                SymbolType.METHOD, SymbolType.CONSTRUCTOR -> ImportanceLevel.MEDIUM
                else -> ImportanceLevel.MEDIUM
            }
        )
    }

    /**
     * 生成符号引用记忆
     *
     * @param node 符号节点
     * @param graph 符号图
     * @return 语义记忆列表
     */
    private fun generateSymbolReferenceMemories(
        node: SymbolNode,
        graph: ai.kastrax.codebase.symbol.model.SymbolGraph
    ): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 获取引用关系
        val referenceRelations = graph.getOutgoingRelations(node.id, SymbolRelationType.REFERENCES)

        if (referenceRelations.isEmpty()) {
            return emptyList()
        }

        // 获取被引用的符号
        val referencedSymbols = referenceRelations.mapNotNull { relation ->
            graph.getNode(relation.targetId)
        }

        // 按类型分组被引用的符号
        val referencedSymbolsByType = referencedSymbols.groupBy { it.type }

        // 为每种类型生成一个记忆
        referencedSymbolsByType.forEach { (type, symbols) ->
            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 引用了 ${symbols.size} 个 ${type.name.lowercase()}:")

                symbols.take(10).forEach { symbol ->
                    append("\n- ${symbol.name}")
                }

                if (symbols.size > 10) {
                    append("\n- ... 等 ${symbols.size - 10} 个")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.SYMBOL_REFERENCE,
                content = content,
                sourceSymbols = listOf(node) + symbols.take(10),
                importance = ImportanceLevel.MEDIUM
            )

            memories.add(memory)
        }

        return memories
    }

    /**
     * 生成继承记忆
     *
     * @param node 符号节点
     * @param graph 符号图
     * @return 语义记忆列表
     */
    private fun generateInheritanceMemories(
        node: SymbolNode,
        graph: ai.kastrax.codebase.symbol.model.SymbolGraph
    ): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 只为类和接口生成继承记忆
        if (node.type !in setOf(SymbolType.CLASS, SymbolType.INTERFACE)) {
            return emptyList()
        }

        // 获取继承关系
        val extendsRelations = graph.getOutgoingRelations(node.id, SymbolRelationType.EXTENDS)
        val extendedByRelations = graph.getIncomingRelations(node.id, SymbolRelationType.EXTENDS)

        // 生成父类记忆
        if (extendsRelations.isNotEmpty()) {
            val parentSymbols = extendsRelations.mapNotNull { relation ->
                graph.getNode(relation.targetId)
            }

            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 继承自:")

                parentSymbols.forEach { symbol ->
                    append("\n- ${symbol.name}")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.INHERITANCE,
                content = content,
                sourceSymbols = listOf(node) + parentSymbols,
                importance = ImportanceLevel.HIGH
            )

            memories.add(memory)
        }

        // 生成子类记忆
        if (extendedByRelations.isNotEmpty()) {
            val childSymbols = extendedByRelations.mapNotNull { relation ->
                graph.getNode(relation.sourceId)
            }

            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 被以下类继承:")

                childSymbols.take(10).forEach { symbol ->
                    append("\n- ${symbol.name}")
                }

                if (childSymbols.size > 10) {
                    append("\n- ... 等 ${childSymbols.size - 10} 个")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.INHERITANCE,
                content = content,
                sourceSymbols = listOf(node) + childSymbols.take(10),
                importance = ImportanceLevel.MEDIUM
            )

            memories.add(memory)
        }

        return memories
    }

    /**
     * 生成实现记忆
     *
     * @param node 符号节点
     * @param graph 符号图
     * @return 语义记忆列表
     */
    private fun generateImplementationMemories(
        node: SymbolNode,
        graph: ai.kastrax.codebase.symbol.model.SymbolGraph
    ): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 获取实现关系
        val implementsRelations = graph.getOutgoingRelations(node.id, SymbolRelationType.IMPLEMENTS)
        val implementedByRelations = graph.getIncomingRelations(node.id, SymbolRelationType.IMPLEMENTS)

        // 生成实现接口记忆
        if (implementsRelations.isNotEmpty() && node.type == SymbolType.CLASS) {
            val interfaceSymbols = implementsRelations.mapNotNull { relation ->
                graph.getNode(relation.targetId)
            }

            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 实现了以下接口:")

                interfaceSymbols.forEach { symbol ->
                    append("\n- ${symbol.name}")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.IMPLEMENTATION,
                content = content,
                sourceSymbols = listOf(node) + interfaceSymbols,
                importance = ImportanceLevel.HIGH
            )

            memories.add(memory)
        }

        // 生成被实现记忆
        if (implementedByRelations.isNotEmpty() && node.type == SymbolType.INTERFACE) {
            val implementingSymbols = implementedByRelations.mapNotNull { relation ->
                graph.getNode(relation.sourceId)
            }

            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 被以下类实现:")

                implementingSymbols.take(10).forEach { symbol ->
                    append("\n- ${symbol.name}")
                }

                if (implementingSymbols.size > 10) {
                    append("\n- ... 等 ${implementingSymbols.size - 10} 个")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.IMPLEMENTATION,
                content = content,
                sourceSymbols = listOf(node) + implementingSymbols.take(10),
                importance = ImportanceLevel.MEDIUM
            )

            memories.add(memory)
        }

        return memories
    }

    /**
     * 生成调用层次结构记忆
     *
     * @param node 符号节点
     * @param graph 符号图
     * @return 语义记忆列表
     */
    private fun generateCallHierarchyMemories(
        node: SymbolNode,
        graph: ai.kastrax.codebase.symbol.model.SymbolGraph
    ): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 只为方法生成调用层次结构记忆
        if (node.type !in setOf(SymbolType.METHOD, SymbolType.CONSTRUCTOR)) {
            return emptyList()
        }

        // 获取调用关系
        val callsRelations = graph.getOutgoingRelations(node.id, SymbolRelationType.CALLS)
        val calledByRelations = graph.getIncomingRelations(node.id, SymbolRelationType.CALLS)

        // 生成调用方法记忆
        if (callsRelations.isNotEmpty()) {
            val calledSymbols = callsRelations.mapNotNull { relation ->
                graph.getNode(relation.targetId)
            }

            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 调用了以下方法:")

                calledSymbols.take(10).forEach { symbol ->
                    append("\n- ${symbol.name}")
                }

                if (calledSymbols.size > 10) {
                    append("\n- ... 等 ${calledSymbols.size - 10} 个")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.CALL_HIERARCHY,
                content = content,
                sourceSymbols = listOf(node) + calledSymbols.take(10),
                importance = ImportanceLevel.MEDIUM
            )

            memories.add(memory)
        }

        // 生成被调用记忆
        if (calledByRelations.isNotEmpty()) {
            val callerSymbols = calledByRelations.mapNotNull { relation ->
                graph.getNode(relation.sourceId)
            }

            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 被以下方法调用:")

                callerSymbols.take(10).forEach { symbol ->
                    append("\n- ${symbol.name}")
                }

                if (callerSymbols.size > 10) {
                    append("\n- ... 等 ${callerSymbols.size - 10} 个")
                }
            }

            val memory = SemanticMemory(
                type = MemoryType.CALL_HIERARCHY,
                content = content,
                sourceSymbols = listOf(node) + callerSymbols.take(10),
                importance = ImportanceLevel.MEDIUM
            )

            memories.add(memory)
        }

        return memories
    }

    /**
     * 生成导入依赖记忆
     *
     * @param node 符号节点
     * @param graph 符号图
     * @return 语义记忆列表
     */
    private fun generateImportDependencyMemories(
        node: SymbolNode,
        graph: ai.kastrax.codebase.symbol.model.SymbolGraph
    ): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 只为类、接口和文件生成导入依赖记忆
        if (node.type !in setOf(SymbolType.CLASS, SymbolType.INTERFACE, SymbolType.MODULE)) {
            return emptyList()
        }

        // 获取导入关系
        val importsRelations = graph.getOutgoingRelations(node.id, SymbolRelationType.IMPORTS)

        // 生成导入记忆
        if (importsRelations.isNotEmpty()) {
            val importedSymbols = importsRelations.mapNotNull { relation ->
                graph.getNode(relation.targetId)
            }

            // 按类型分组导入的符号
            val importedSymbolsByType = importedSymbols.groupBy { it.type }

            // 为每种类型生成一个记忆
            importedSymbolsByType.forEach { (type, symbols) ->
                val content = buildString {
                    append("${node.type.name.lowercase()} ${node.name} 导入了 ${symbols.size} 个 ${type.name.lowercase()}:")

                    symbols.take(10).forEach { symbol ->
                        append("\n- ${symbol.name}")
                    }

                    if (symbols.size > 10) {
                        append("\n- ... 等 ${symbols.size - 10} 个")
                    }
                }

                val memory = SemanticMemory(
                    type = MemoryType.IMPORT_DEPENDENCY,
                    content = content,
                    sourceSymbols = listOf(node) + symbols.take(10),
                    importance = ImportanceLevel.MEDIUM
                )

                memories.add(memory)
            }
        }

        return memories
    }

    /**
     * 生成库使用记忆
     *
     * @param node 符号节点
     * @param librarySymbols 库符号列表
     * @return 语义记忆
     */
    fun generateLibraryUsageMemory(
        node: SymbolNode,
        librarySymbols: List<SymbolNode>
    ): SemanticMemory? {
        // 只为类和接口生成库使用记忆
        if (node.type !in setOf(SymbolType.CLASS, SymbolType.INTERFACE)) {
            return null
        }

        if (librarySymbols.isEmpty()) {
            return null
        }

        val content = buildString {
            append("${node.type.name.lowercase()} ${node.name} 使用了以下库:")

            // 按库分组符号
            val symbolsByLibrary = librarySymbols.groupBy { symbol ->
                symbol.qualifiedName.split('.').take(2).joinToString(".")
            }

            symbolsByLibrary.entries.take(10).forEach { (library, symbols) ->
                append("\n- $library (${symbols.size} 个符号)")
            }

            if (symbolsByLibrary.size > 10) {
                append("\n- ... 等 ${symbolsByLibrary.size - 10} 个库")
            }
        }

        return SemanticMemory(
            type = MemoryType.LIBRARY_USAGE,
            content = content,
            sourceSymbols = listOf(node) + librarySymbols.take(10),
            importance = ImportanceLevel.MEDIUM
        )
    }

    /**
     * 生成语义关系记忆
     *
     * @param sourceNode 源符号节点
     * @param targetNode 目标符号节点
     * @param relationType 关系类型
     * @param description 关系描述
     * @return 语义记忆
     */
    fun generateSemanticRelationMemory(
        sourceNode: SymbolNode,
        targetNode: SymbolNode,
        relationType: SymbolRelationType,
        description: String
    ): SemanticMemory {
        val content = buildString {
            append("${sourceNode.type.name.lowercase()} ${sourceNode.name} ")
            append(description)
            append(" ${targetNode.type.name.lowercase()} ${targetNode.name}")
        }

        return SemanticMemory(
            type = MemoryType.SEMANTIC_RELATION,
            content = content,
            sourceSymbols = listOf(sourceNode, targetNode),
            importance = ImportanceLevel.MEDIUM,
            metadata = mutableMapOf(
                "relationType" to relationType.name
            )
        )
    }

    /**
     * 生成自定义记忆
     *
     * @param content 记忆内容
     * @param sourceElements 源代码元素列表
     * @param sourceSymbols 源符号节点列表
     * @param importance 重要性级别
     * @param metadata 元数据
     * @return 语义记忆
     */
    fun generateCustomMemory(
        content: String,
        sourceElements: List<CodeElement> = emptyList(),
        sourceSymbols: List<SymbolNode> = emptyList(),
        importance: ImportanceLevel = ImportanceLevel.MEDIUM,
        metadata: Map<String, Any> = emptyMap()
    ): SemanticMemory {
        return SemanticMemory(
            type = MemoryType.CUSTOM,
            content = content,
            sourceElements = sourceElements,
            sourceSymbols = sourceSymbols,
            importance = importance,
            metadata = metadata.toMutableMap()
        )
    }
}
