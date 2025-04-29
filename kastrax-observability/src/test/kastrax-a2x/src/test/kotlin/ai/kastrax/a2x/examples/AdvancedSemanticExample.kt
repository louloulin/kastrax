package ai.kastrax.a2x.examples

import ai.kastrax.a2x.multimodal.*
import ai.kastrax.a2x.semantic.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 高级语义理解示例
 */
fun main() {
    println("=== 高级语义理解示例 ===")
    
    // 创建多模态处理器
    val multimodalProcessor = MultimodalProcessor()
    
    // 注册处理器
    multimodalProcessor.registerModalityProcessor(TextProcessor())
    multimodalProcessor.registerModalityProcessor(ImageProcessor())
    multimodalProcessor.registerModalityProcessor(AudioProcessor())
    multimodalProcessor.registerModalityProcessor(VideoProcessor())
    
    // 注册融合策略
    multimodalProcessor.registerFusionStrategy("simple", SimpleFusionStrategy())
    multimodalProcessor.registerFusionStrategy("weighted", WeightedFusionStrategy())
    
    // 设置默认融合策略
    multimodalProcessor.setFusionStrategy("weighted")
    
    // 创建语义服务
    val semanticService = SemanticService()
    
    // 配置本体
    configureOntology(semanticService.ontologyManager)
    
    // 配置意图
    configureIntents(semanticService.intentRecognizer)
    
    // 配置实体
    configureEntities(semanticService.entityResolver)
    
    // 创建上下文
    val context = semanticService.createContext(
        name = "多模态对话上下文",
        description = "用户与系统的多模态对话上下文",
        type = "multimodal_conversation"
    )
    
    // 创建会话
    val session = semanticService.createSession(
        contextId = context.id,
        entityId = "user-123"
    )
    
    // 更新上下文数据
    semanticService.updateContextData(
        contextId = context.id,
        data = buildJsonObject {
            put("location", "office")
            put("time", "morning")
            put("device", "desktop")
        }
    )
    
    // 处理多模态输入
    val input = MultimodalInput(
        modalityInputs = mapOf(
            "text" to JsonPrimitive("我在2023-05-15拍摄了这张照片，请联系我的邮箱user@example.com获取更多信息"),
            "image" to JsonPrimitive("https://example.com/office_photo.jpg")
        ),
        context = context
    )
    
    println("\n1. 处理多模态输入")
    val result = multimodalProcessor.processMultimodalInput(input)
    
    // 打印结果
    println("处理结果:")
    println("- 模态数量: ${result.modalityResults.size}")
    println("- 提取实体数量: ${result.entities.size}")
    println("- 推理关系数量: ${result.relationships.size}")
    
    // 打印实体
    println("\n提取的实体:")
    result.entities.forEach { entity ->
        println("- ${entity.type}: ${entity.value} (置信度: ${entity.confidence})")
    }
    
    // 分析跨模态关系
    println("\n2. 分析跨模态关系")
    val relationships = multimodalProcessor.analyzeCrossModalRelationships(input, context)
    
    // 打印关系
    println("跨模态关系:")
    relationships.forEach { relationship ->
        println("- ${relationship.sourceModality} ${relationship.type} ${relationship.targetModality} (强度: ${relationship.strength})")
    }
    
    // 处理文本消息
    println("\n3. 处理文本消息")
    val messageResult = semanticService.processMessage(
        text = "我想查看2023-05-15拍摄的照片",
        sessionId = session.id
    )
    
    // 打印意图和实体
    println("识别的意图: ${messageResult.intent?.name ?: "未识别"} (置信度: ${messageResult.confidence})")
    println("提取的实体:")
    messageResult.entities.forEach { entity ->
        println("- ${entity.type}: ${entity.value} (置信度: ${entity.confidence})")
    }
    
    // 关系推理
    println("\n4. 关系推理")
    val relationshipEngine = multimodalProcessor.relationshipEngine
    
    // 注册关系类型
    val containsRelationType = RelationshipType(
        id = "rel-type-contains",
        name = "contains",
        description = "表示一个实体包含另一个实体",
        symmetric = false,
        transitive = true
    )
    relationshipEngine.registerRelationshipType(containsRelationType)
    
    // 添加关系规则
    val proximityRule = RelationshipRule(
        id = "rule-proximity",
        type = "proximity",
        description = "基于文本中的位置关系推断实体关系",
        parameters = mapOf(
            "maxDistance" to JsonPrimitive(20)
        )
    )
    relationshipEngine.addRelationshipRule(containsRelationType.id, proximityRule)
    
    // 推理关系
    val inferredRelationships = relationshipEngine.inferRelationships(result.entities, context)
    
    // 打印推理的关系
    println("推理的关系:")
    inferredRelationships.forEach { relationship ->
        val sourceEntity = result.entities.find { it.id == relationship.sourceEntityId }
        val targetEntity = result.entities.find { it.id == relationship.targetEntityId }
        
        println("- ${sourceEntity?.type}(${sourceEntity?.value}) ${relationship.typeId} ${targetEntity?.type}(${targetEntity?.value}) (置信度: ${relationship.confidence})")
    }
    
    // 计算关系强度
    if (result.entities.size >= 2) {
        val entity1 = result.entities[0]
        val entity2 = result.entities[1]
        
        val strength = relationshipEngine.calculateRelationshipStrength(entity1.id, entity2.id)
        println("\n实体 ${entity1.type}(${entity1.value}) 和 ${entity2.type}(${entity2.value}) 之间的关系强度: $strength")
    }
    
    println("\n=== 示例结束 ===")
}

/**
 * 配置本体
 */
private fun configureOntology(ontologyManager: OntologyManager) {
    // 创建概念
    val personConcept = Concept(
        id = "concept-person",
        name = "人物",
        description = "表示一个人",
        properties = mapOf(
            "name" to "string",
            "age" to "integer",
            "email" to "string"
        )
    )
    
    val locationConcept = Concept(
        id = "concept-location",
        name = "地点",
        description = "表示一个地点",
        properties = mapOf(
            "name" to "string",
            "address" to "string",
            "coordinates" to "string"
        )
    )
    
    val eventConcept = Concept(
        id = "concept-event",
        name = "事件",
        description = "表示一个事件",
        properties = mapOf(
            "name" to "string",
            "date" to "date",
            "location" to "string"
        )
    )
    
    // 创建关系
    val locatedAtRelation = Relation(
        id = "relation-located-at",
        name = "位于",
        description = "表示一个实体位于某个地点",
        sourceConcept = personConcept.id,
        targetConcept = locationConcept.id,
        properties = mapOf(
            "since" to "date",
            "until" to "date"
        )
    )
    
    val participatesInRelation = Relation(
        id = "relation-participates-in",
        name = "参与",
        description = "表示一个人参与了某个事件",
        sourceConcept = personConcept.id,
        targetConcept = eventConcept.id,
        properties = mapOf(
            "role" to "string"
        )
    )
    
    val hostedAtRelation = Relation(
        id = "relation-hosted-at",
        name = "举办于",
        description = "表示一个事件在某个地点举办",
        sourceConcept = eventConcept.id,
        targetConcept = locationConcept.id,
        properties = mapOf()
    )
    
    // 创建本体
    val ontology = Ontology(
        id = "ontology-general",
        name = "通用本体",
        description = "包含通用概念和关系的本体",
        version = "1.0",
        concepts = listOf(personConcept, locationConcept, eventConcept),
        relations = listOf(locatedAtRelation, participatesInRelation, hostedAtRelation)
    )
    
    // 注册本体
    ontologyManager.registerOntology(ontology)
}

/**
 * 配置意图
 */
private fun configureIntents(intentRecognizer: IntentRecognizer) {
    // 创建查询意图
    val queryIntent = intentRecognizer.createIntent(
        name = "查询",
        description = "查询信息的意图"
    )
    
    // 添加查询意图的模式
    intentRecognizer.addIntentPattern(
        intentId = queryIntent.id,
        type = "keyword",
        pattern = "查询 查看 搜索 找 获取"
    )
    
    intentRecognizer.addIntentPattern(
        intentId = queryIntent.id,
        type = "regex",
        pattern = ".*[查询|查看|搜索|找|获取].*"
    )
    
    // 创建创建意图
    val createIntent = intentRecognizer.createIntent(
        name = "创建",
        description = "创建信息的意图"
    )
    
    // 添加创建意图的模式
    intentRecognizer.addIntentPattern(
        intentId = createIntent.id,
        type = "keyword",
        pattern = "创建 新建 添加 建立 生成"
    )
    
    intentRecognizer.addIntentPattern(
        intentId = createIntent.id,
        type = "regex",
        pattern = ".*[创建|新建|添加|建立|生成].*"
    )
    
    // 创建更新意图
    val updateIntent = intentRecognizer.createIntent(
        name = "更新",
        description = "更新信息的意图"
    )
    
    // 添加更新意图的模式
    intentRecognizer.addIntentPattern(
        intentId = updateIntent.id,
        type = "keyword",
        pattern = "更新 修改 编辑 改变"
    )
    
    intentRecognizer.addIntentPattern(
        intentId = updateIntent.id,
        type = "regex",
        pattern = ".*[更新|修改|编辑|改变].*"
    )
    
    // 创建删除意图
    val deleteIntent = intentRecognizer.createIntent(
        name = "删除",
        description = "删除信息的意图"
    )
    
    // 添加删除意图的模式
    intentRecognizer.addIntentPattern(
        intentId = deleteIntent.id,
        type = "keyword",
        pattern = "删除 移除 清除"
    )
    
    intentRecognizer.addIntentPattern(
        intentId = deleteIntent.id,
        type = "regex",
        pattern = ".*[删除|移除|清除].*"
    )
}

/**
 * 配置实体
 */
private fun configureEntities(entityResolver: EntityResolver) {
    // 创建日期提取器
    entityResolver.registerExtractor(
        EntityExtractor(
            id = "extractor-date",
            type = "date",
            extractorType = "regex",
            pattern = "\\b\\d{4}-\\d{2}-\\d{2}\\b",
            priority = 1.0
        )
    )
    
    // 创建邮箱提取器
    entityResolver.registerExtractor(
        EntityExtractor(
            id = "extractor-email",
            type = "email",
            extractorType = "regex",
            pattern = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b",
            priority = 1.0
        )
    )
    
    // 创建URL提取器
    entityResolver.registerExtractor(
        EntityExtractor(
            id = "extractor-url",
            type = "url",
            extractorType = "regex",
            pattern = "\\bhttps?://[^\\s]+\\b",
            priority = 1.0
        )
    )
    
    // 创建人名提取器
    entityResolver.registerExtractor(
        EntityExtractor(
            id = "extractor-person",
            type = "person",
            extractorType = "dictionary",
            dictionary = listOf("张三", "李四", "王五", "赵六"),
            priority = 0.8
        )
    )
    
    // 创建地点提取器
    entityResolver.registerExtractor(
        EntityExtractor(
            id = "extractor-location",
            type = "location",
            extractorType = "dictionary",
            dictionary = listOf("北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆"),
            priority = 0.8
        )
    )
}
