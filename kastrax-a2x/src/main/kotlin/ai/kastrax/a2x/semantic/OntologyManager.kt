package ai.kastrax.a2x.semantic

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.concurrent.ConcurrentHashMap

/**
 * 本体管理器，负责管理领域本体和概念
 */
class OntologyManager {
    /**
     * 本体映射
     */
    private val ontologies = ConcurrentHashMap<String, Ontology>()
    
    /**
     * 概念映射
     */
    private val concepts = ConcurrentHashMap<String, Concept>()
    
    /**
     * 关系映射
     */
    private val relations = ConcurrentHashMap<String, Relation>()
    
    /**
     * 注册本体
     */
    fun registerOntology(ontology: Ontology) {
        ontologies[ontology.id] = ontology
        
        // 注册本体中的概念
        ontology.concepts.forEach { concept ->
            concepts[concept.id] = concept
        }
        
        // 注册本体中的关系
        ontology.relations.forEach { relation ->
            relations[relation.id] = relation
        }
    }
    
    /**
     * 注销本体
     */
    fun unregisterOntology(ontologyId: String) {
        val ontology = ontologies.remove(ontologyId) ?: return
        
        // 注销本体中的概念
        ontology.concepts.forEach { concept ->
            concepts.remove(concept.id)
        }
        
        // 注销本体中的关系
        ontology.relations.forEach { relation ->
            relations.remove(relation.id)
        }
    }
    
    /**
     * 获取本体
     */
    fun getOntology(ontologyId: String): Ontology? {
        return ontologies[ontologyId]
    }
    
    /**
     * 获取所有本体
     */
    fun getAllOntologies(): List<Ontology> {
        return ontologies.values.toList()
    }
    
    /**
     * 注册概念
     */
    fun registerConcept(concept: Concept) {
        concepts[concept.id] = concept
    }
    
    /**
     * 注销概念
     */
    fun unregisterConcept(conceptId: String) {
        concepts.remove(conceptId)
    }
    
    /**
     * 获取概念
     */
    fun getConcept(conceptId: String): Concept? {
        return concepts[conceptId]
    }
    
    /**
     * 获取所有概念
     */
    fun getAllConcepts(): List<Concept> {
        return concepts.values.toList()
    }
    
    /**
     * 注册关系
     */
    fun registerRelation(relation: Relation) {
        relations[relation.id] = relation
    }
    
    /**
     * 注销关系
     */
    fun unregisterRelation(relationId: String) {
        relations.remove(relationId)
    }
    
    /**
     * 获取关系
     */
    fun getRelation(relationId: String): Relation? {
        return relations[relationId]
    }
    
    /**
     * 获取所有关系
     */
    fun getAllRelations(): List<Relation> {
        return relations.values.toList()
    }
    
    /**
     * 查询概念
     */
    fun queryConcepts(query: String): List<Concept> {
        // 简单的字符串匹配查询
        return concepts.values.filter { concept ->
            concept.name.contains(query, ignoreCase = true) ||
            concept.description.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 查询关系
     */
    fun queryRelations(query: String): List<Relation> {
        // 简单的字符串匹配查询
        return relations.values.filter { relation ->
            relation.name.contains(query, ignoreCase = true) ||
            relation.description.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 获取概念的关系
     */
    fun getConceptRelations(conceptId: String): List<Relation> {
        return relations.values.filter { relation ->
            relation.sourceConcept == conceptId || relation.targetConcept == conceptId
        }
    }
    
    /**
     * 获取相关概念
     */
    fun getRelatedConcepts(conceptId: String): List<Concept> {
        val relatedConceptIds = getConceptRelations(conceptId).flatMap { relation ->
            listOfNotNull(
                if (relation.sourceConcept != conceptId) relation.sourceConcept else null,
                if (relation.targetConcept != conceptId) relation.targetConcept else null
            )
        }.toSet()
        
        return relatedConceptIds.mapNotNull { id -> concepts[id] }
    }
}

/**
 * 本体
 */
@Serializable
data class Ontology(
    /**
     * 本体 ID
     */
    val id: String,
    
    /**
     * 本体名称
     */
    val name: String,
    
    /**
     * 本体描述
     */
    val description: String,
    
    /**
     * 本体版本
     */
    val version: String,
    
    /**
     * 本体概念列表
     */
    val concepts: List<Concept>,
    
    /**
     * 本体关系列表
     */
    val relations: List<Relation>,
    
    /**
     * 本体元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 概念
 */
@Serializable
data class Concept(
    /**
     * 概念 ID
     */
    val id: String,
    
    /**
     * 概念名称
     */
    val name: String,
    
    /**
     * 概念描述
     */
    val description: String,
    
    /**
     * 概念类型
     */
    val type: String,
    
    /**
     * 概念属性
     */
    val properties: List<Property> = emptyList(),
    
    /**
     * 概念元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 属性
 */
@Serializable
data class Property(
    /**
     * 属性名称
     */
    val name: String,
    
    /**
     * 属性类型
     */
    val type: String,
    
    /**
     * 属性描述
     */
    val description: String,
    
    /**
     * 属性是否必需
     */
    val required: Boolean = false,
    
    /**
     * 属性默认值
     */
    val defaultValue: String? = null,
    
    /**
     * 属性元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 关系
 */
@Serializable
data class Relation(
    /**
     * 关系 ID
     */
    val id: String,
    
    /**
     * 关系名称
     */
    val name: String,
    
    /**
     * 关系描述
     */
    val description: String,
    
    /**
     * 关系类型
     */
    val type: String,
    
    /**
     * 源概念 ID
     */
    val sourceConcept: String,
    
    /**
     * 目标概念 ID
     */
    val targetConcept: String,
    
    /**
     * 关系属性
     */
    val properties: List<Property> = emptyList(),
    
    /**
     * 关系元数据
     */
    val metadata: Map<String, String> = emptyMap()
)
