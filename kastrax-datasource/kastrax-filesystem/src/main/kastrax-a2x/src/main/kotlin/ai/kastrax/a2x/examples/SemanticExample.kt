package ai.kastrax.a2x.examples

import ai.kastrax.a2x.semantic.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 语义示例
 */
fun main() {
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
        name = "对话上下文",
        description = "用户与系统的对话上下文",
        type = "conversation"
    )
    
    // 创建会话
    val session = semanticService.createSession(
        contextId = context.id,
        entityId = "user-123"
    )
    
    if (session != null) {
        // 处理消息
        val messages = listOf(
            "你好，我想查询天气",
            "北京明天的天气怎么样？",
            "谢谢，再见"
        )
        
        messages.forEach { message ->
            println("\n用户: $message")
            
            // 处理消息
            val result = semanticService.processMessage(message, session.id)
            
            // 打印意图
            println("识别的意图: ${result.intentResult.topIntent?.name ?: "未识别"} (置信度: ${result.intentResult.topConfidence})")
            
            // 打印实体
            result.entities.forEach { (type, entities) ->
                println("识别的实体 ($type): ${entities.map { it.text }}")
            }
            
            // 生成响应
            val response = semanticService.generateResponse(result, session.id)
            println("系统: ${response.text}")
        }
        
        // 打印会话历史
        val updatedSession = semanticService.getSession(session.id)
        println("\n会话历史:")
        updatedSession?.history?.forEach { entry ->
            val text = when (entry.type) {
                "user" -> "用户: ${(entry.content as? kotlinx.serialization.json.JsonObject)?.get("text")?.toString()?.replace("\"", "")}"
                "system" -> "系统: ${(entry.content as? kotlinx.serialization.json.JsonObject)?.get("text")?.toString()?.replace("\"", "")}"
                else -> "${entry.type}: ${entry.content}"
            }
            println(text)
        }
    }
}

/**
 * 配置本体
 */
private fun configureOntology(ontologyManager: OntologyManager) {
    // 创建天气本体
    val weatherConcept = Concept(
        id = "concept-weather",
        name = "天气",
        description = "天气概念",
        type = "entity",
        properties = listOf(
            Property(
                name = "temperature",
                type = "number",
                description = "温度"
            ),
            Property(
                name = "condition",
                type = "string",
                description = "天气状况"
            ),
            Property(
                name = "humidity",
                type = "number",
                description = "湿度"
            )
        )
    )
    
    val locationConcept = Concept(
        id = "concept-location",
        name = "位置",
        description = "位置概念",
        type = "entity",
        properties = listOf(
            Property(
                name = "name",
                type = "string",
                description = "位置名称"
            ),
            Property(
                name = "latitude",
                type = "number",
                description = "纬度"
            ),
            Property(
                name = "longitude",
                type = "number",
                description = "经度"
            )
        )
    )
    
    val timeConcept = Concept(
        id = "concept-time",
        name = "时间",
        description = "时间概念",
        type = "entity",
        properties = listOf(
            Property(
                name = "date",
                type = "string",
                description = "日期"
            ),
            Property(
                name = "time",
                type = "string",
                description = "时间"
            )
        )
    )
    
    val hasWeatherRelation = Relation(
        id = "relation-has-weather",
        name = "有天气",
        description = "位置有天气",
        type = "association",
        sourceConcept = "concept-location",
        targetConcept = "concept-weather"
    )
    
    val atTimeRelation = Relation(
        id = "relation-at-time",
        name = "在时间",
        description = "天气在时间",
        type = "association",
        sourceConcept = "concept-weather",
        targetConcept = "concept-time"
    )
    
    val weatherOntology = Ontology(
        id = "ontology-weather",
        name = "天气本体",
        description = "天气领域本体",
        version = "1.0.0",
        concepts = listOf(weatherConcept, locationConcept, timeConcept),
        relations = listOf(hasWeatherRelation, atTimeRelation)
    )
    
    // 注册本体
    ontologyManager.registerOntology(weatherOntology)
}

/**
 * 配置意图
 */
private fun configureIntents(intentRecognizer: IntentRecognizer) {
    // 创建问候意图
    val greetingIntent = intentRecognizer.createIntent(
        name = "问候",
        description = "用户问候"
    )
    
    // 添加问候意图模式
    intentRecognizer.addIntentPattern(
        intentId = greetingIntent.id,
        type = "keyword",
        pattern = "你好,您好,早上好,下午好,晚上好,嗨,哈喽,hello,hi"
    )
    
    // 创建天气查询意图
    val weatherQueryIntent = intentRecognizer.createIntent(
        name = "天气查询",
        description = "查询天气"
    )
    
    // 添加天气查询意图模式
    intentRecognizer.addIntentPattern(
        intentId = weatherQueryIntent.id,
        type = "keyword",
        pattern = "天气,气温,温度,下雨,下雪,晴天,阴天,多云"
    )
    
    // 创建告别意图
    val farewellIntent = intentRecognizer.createIntent(
        name = "告别",
        description = "用户告别"
    )
    
    // 添加告别意图模式
    intentRecognizer.addIntentPattern(
        intentId = farewellIntent.id,
        type = "keyword",
        pattern = "再见,拜拜,拜,goodbye,bye,下次见,回头见"
    )
}

/**
 * 配置实体
 */
private fun configureEntities(entityResolver: EntityResolver) {
    // 创建位置实体类型
    val locationEntityType = entityResolver.createEntityType(
        name = "位置",
        description = "位置实体"
    )
    
    // 创建位置字典提取器
    entityResolver.createDictionaryExtractor(
        entityTypeId = locationEntityType.id,
        name = "城市提取器",
        entries = listOf("北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "重庆", "武汉", "西安")
    )
    
    // 创建时间实体类型
    val timeEntityType = entityResolver.createEntityType(
        name = "时间",
        description = "时间实体"
    )
    
    // 创建时间字典提取器
    entityResolver.createDictionaryExtractor(
        entityTypeId = timeEntityType.id,
        name = "时间提取器",
        entries = listOf("今天", "明天", "后天", "昨天", "前天", "上午", "下午", "晚上", "早上", "中午")
    )
    
    // 创建天气实体类型
    val weatherEntityType = entityResolver.createEntityType(
        name = "天气",
        description = "天气实体"
    )
    
    // 创建天气字典提取器
    entityResolver.createDictionaryExtractor(
        entityTypeId = weatherEntityType.id,
        name = "天气提取器",
        entries = listOf("晴天", "阴天", "多云", "下雨", "下雪", "雾", "霾", "冰雹", "台风", "龙卷风")
    )
}
