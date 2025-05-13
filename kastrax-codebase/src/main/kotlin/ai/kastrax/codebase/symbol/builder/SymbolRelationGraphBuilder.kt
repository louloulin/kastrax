package ai.kastrax.codebase.symbol.builder

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.relation.CodeRelation
import ai.kastrax.codebase.semantic.relation.RelationType
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelation
import ai.kastrax.codebase.symbol.model.SymbolRelationGraph
import ai.kastrax.codebase.symbol.model.SymbolRelationGraphConfig
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType

/**
 * 符号关系图构建器
 *
 * 用于从代码元素和关系构建符号关系图
 */
class SymbolRelationGraphBuilder {
    /**
     * 从代码元素和关系构建符号关系图
     *
     * @param elements 代码元素集合
     * @param relations 代码关系集合
     * @param config 符号关系图配置
     * @return 符号关系图
     */
    fun buildGraph(
        elements: Collection<CodeElement>,
        relations: Collection<CodeRelation>,
        config: SymbolRelationGraphConfig = SymbolRelationGraphConfig()
    ): SymbolRelationGraph {
        val graph = SymbolRelationGraph()

        // 添加节点
        elements.forEach { element ->
            if (shouldIncludeElement(element, config)) {
                val node = createSymbolNode(element)
                graph.addNode(node)
            }
        }

        // 添加边
        relations.forEach { relation ->
            if (shouldIncludeRelation(relation.type, config)) {
                val sourceNode = graph.getNodeById(relation.sourceId)
                val targetNode = graph.getNodeById(relation.targetId)

                if (sourceNode != null && targetNode != null) {
                    val edge = createSymbolRelation(relation)
                    graph.addRelation(edge)
                }
            }
        }

        return graph
    }

    /**
     * 创建符号节点
     *
     * @param element 代码元素
     * @return 符号节点
     */
    private fun createSymbolNode(element: CodeElement): SymbolNode {
        return SymbolNode(
            id = element.id,
            name = element.name,
            qualifiedName = element.qualifiedName,
            type = convertToSymbolType(element.type),
            kind = "DEFINITION",
            location = element.location,
            codeElement = element,
            metadata = element.metadata.toMutableMap()
        )
    }

    /**
     * 创建符号关系
     *
     * @param relation 代码关系
     * @return 符号关系
     */
    private fun createSymbolRelation(relation: CodeRelation): SymbolRelation {
        return SymbolRelation(
            id = relation.id,
            sourceId = relation.sourceId,
            targetId = relation.targetId,
            type = convertToSymbolRelationType(relation.type),
            metadata = relation.metadata.toMutableMap()
        )
    }

    /**
     * 将代码元素类型转换为符号类型
     *
     * @param codeElementType 代码元素类型
     * @return 符号类型
     */
    private fun convertToSymbolType(codeElementType: CodeElementType): SymbolType {
        return when (codeElementType) {
            CodeElementType.FILE -> SymbolType.FILE
            CodeElementType.PACKAGE -> SymbolType.PACKAGE
            CodeElementType.CLASS -> SymbolType.CLASS
            CodeElementType.INTERFACE -> SymbolType.INTERFACE
            CodeElementType.ENUM -> SymbolType.ENUM
            CodeElementType.ANNOTATION -> SymbolType.ANNOTATION
            CodeElementType.METHOD -> SymbolType.METHOD
            CodeElementType.CONSTRUCTOR -> SymbolType.CONSTRUCTOR
            CodeElementType.FIELD -> SymbolType.FIELD
            CodeElementType.PROPERTY -> SymbolType.PROPERTY
            CodeElementType.PARAMETER -> SymbolType.PARAMETER
            CodeElementType.FUNCTION -> SymbolType.FUNCTION
            CodeElementType.VARIABLE -> SymbolType.LOCAL_VARIABLE
            CodeElementType.LOCAL_VARIABLE -> SymbolType.LOCAL_VARIABLE
            CodeElementType.IMPORT -> SymbolType.IMPORT
            CodeElementType.NAMESPACE -> SymbolType.NAMESPACE
            CodeElementType.MODULE -> SymbolType.MODULE
            CodeElementType.LAMBDA -> SymbolType.LAMBDA
            CodeElementType.BLOCK -> SymbolType.BLOCK
            CodeElementType.STATEMENT -> SymbolType.STATEMENT
            CodeElementType.EXPRESSION -> SymbolType.EXPRESSION
            CodeElementType.COMMENT -> SymbolType.COMMENT
            CodeElementType.UNKNOWN -> SymbolType.UNKNOWN
            else -> SymbolType.UNKNOWN
        }
    }

    /**
     * 将代码关系类型转换为符号关系类型
     *
     * @param relationType 代码关系类型
     * @return 符号关系类型
     */
    private fun convertToSymbolRelationType(relationType: RelationType): SymbolRelationType {
        return when (relationType) {
            RelationType.INHERITANCE -> SymbolRelationType.EXTENDS
            RelationType.IMPLEMENTATION -> SymbolRelationType.IMPLEMENTS
            RelationType.USAGE -> SymbolRelationType.USES
            RelationType.DEPENDENCY -> SymbolRelationType.DEPENDS_ON
            RelationType.OVERRIDE -> SymbolRelationType.OVERRIDES
            RelationType.REFERENCE -> SymbolRelationType.REFERENCES
            RelationType.IMPORT -> SymbolRelationType.IMPORTS
        }
    }

    /**
     * 判断是否应该包含元素
     *
     * @param element 代码元素
     * @param config 符号关系图配置
     * @return 是否应该包含
     */
    private fun shouldIncludeElement(element: CodeElement, config: SymbolRelationGraphConfig): Boolean {
        return when (element.type) {
            CodeElementType.FILE -> config.includeFiles
            CodeElementType.PACKAGE -> config.includePackages
            CodeElementType.CLASS -> config.includeClasses
            CodeElementType.INTERFACE -> config.includeInterfaces
            CodeElementType.ENUM -> config.includeEnums
            CodeElementType.ANNOTATION -> config.includeAnnotations
            CodeElementType.METHOD -> config.includeMethods
            CodeElementType.CONSTRUCTOR -> config.includeConstructors
            CodeElementType.FIELD -> config.includeFields
            CodeElementType.PROPERTY -> config.includeProperties
            CodeElementType.PARAMETER -> config.includeParameters
            CodeElementType.FUNCTION -> config.includeFunctions
            CodeElementType.VARIABLE -> config.includeVariables
            CodeElementType.IMPORT -> config.includeImports
            CodeElementType.NAMESPACE -> config.includeNamespaces
            CodeElementType.MODULE -> config.includeModules
            CodeElementType.UNKNOWN -> false
        }
    }

    /**
     * 判断是否应该包含关系
     *
     * @param type 关系类型
     * @param config 符号关系图配置
     * @return 是否应该包含
     */
    private fun shouldIncludeRelation(type: RelationType, config: SymbolRelationGraphConfig): Boolean {
        return when (type) {
            RelationType.INHERITANCE -> config.includeInheritance
            RelationType.IMPLEMENTATION -> config.includeImplementation
            RelationType.USAGE -> config.includeUsage
            RelationType.DEPENDENCY -> config.includeDependency
            RelationType.OVERRIDE -> config.includeOverride
            RelationType.REFERENCE -> config.includeReference
            RelationType.IMPORT -> config.includeImport
        }
    }
}
