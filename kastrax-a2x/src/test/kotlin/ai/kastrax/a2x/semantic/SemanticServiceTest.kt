package ai.kastrax.a2x.semantic

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 语义服务测试
 */
class SemanticServiceTest {
    /**
     * 语义服务
     */
    private lateinit var semanticService: SemanticService
    
    @BeforeEach
    fun setup() {
        // 创建语义服务
        semanticService = SemanticService()
        
        // 配置本体
        configureOntology()
        
        // 配置意图
        configureIntents()
        
        // 配置实体
        configureEntities()
    }
    
    @Test
    fun `test ontology management`() {
        // 获取所有本体
        val ontologies = semanticService.ontologyManager.getAllOntologies()
        
        // 验证本体
        assertEquals(1, ontologies.size)
        assertEquals("天气本体", ontologies[0].name)
        
        // 获取所有概念
        val concepts = semanticService.ontologyManager.getAllConcepts()
        
        // 验证概念
        assertEquals(3, concepts.size)
        assertTrue(concepts.any { it.name == "天气" })
        assertTrue(concepts.any { it.name == "位置" })
        assertTrue(concepts.any { it.name == "时间" })
        
        // 获取所有关系
        val relations = semanticService.ontologyManager.getAllRelations()
        
        // 验证关系
        assertEquals(2, relations.size)
        assertTrue(relations.any { it.name == "有天气" })
        assertTrue(relations.any { it.name == "在时间" })
    }
    
    @Test
    fun `test intent recognition`() {
        // 识别问候意图
        val greetingResult = semanticService.intentRecognizer.recognizeIntent("你好，请问今天天气怎么样？")
        
        // 验证问候意图
        assertNotNull(greetingResult.topIntent)
        assertEquals("问候", greetingResult.topIntent?.name)
        assertTrue(greetingResult.topConfidence > 0.0)
        
        // 识别天气查询意图
        val weatherQueryResult = semanticService.intentRecognizer.recognizeIntent("北京明天的天气如何？")
        
        // 验证天气查询意图
        assertNotNull(weatherQueryResult.topIntent)
        assertEquals("天气查询", weatherQueryResult.topIntent?.name)
        assertTrue(weatherQueryResult.topConfidence > 0.0)
        
        // 识别告别意图
        val farewellResult = semanticService.intentRecognizer.recognizeIntent("谢谢，再见！")
        
        // 验证告别意图
        assertNotNull(farewellResult.topIntent)
        assertEquals("告别", farewellResult.topIntent?.name)
        assertTrue(farewellResult.topConfidence > 0.0)
    }
    
    @Test
    fun `test entity resolution`() {
        // 解析实体
        val entities = semanticService.entityResolver.resolveEntities("北京明天的天气如何？")
        
        // 验证实体
        assertTrue(entities.isNotEmpty())
        
        // 验证位置实体
        val locationEntityType = semanticService.entityResolver.getAllEntityTypes().find { it.name == "位置" }
        assertNotNull(locationEntityType)
        val locationEntities = entities[locationEntityType.id]
        assertNotNull(locationEntities)
        assertEquals(1, locationEntities.size)
        assertEquals("北京", locationEntities[0].text)
        
        // 验证时间实体
        val timeEntityType = semanticService.entityResolver.getAllEntityTypes().find { it.name == "时间" }
        assertNotNull(timeEntityType)
        val timeEntities = entities[timeEntityType.id]
        assertNotNull(timeEntities)
        assertEquals(1, timeEntities.size)
        assertEquals("明天", timeEntities[0].text)
    }
    
    @Test
    fun `test context management`() {
        // 创建上下文
        val context = semanticService.createContext(
            name = "对话上下文",
            description = "用户与系统的对话上下文",
            type = "conversation"
        )
        
        // 验证上下文
        assertNotNull(context)
        assertEquals("对话上下文", context.name)
        
        // 创建会话
        val session = semanticService.createSession(
            contextId = context.id,
            entityId = "user-123"
        )
        
        // 验证会话
        assertNotNull(session)
        assertEquals(context.id, session.contextId)
        assertEquals("user-123", session.entityId)
        
        // 处理消息
        val result = semanticService.processMessage("你好，请问北京明天的天气如何？", session.id)
        
        // 验证处理结果
        assertNotNull(result)
        assertNotNull(result.intentResult.topIntent)
        assertTrue(result.entities.isNotEmpty())
        
        // 生成响应
        val response = semanticService.generateResponse(result, session.id)
        
        // 验证响应
        assertNotNull(response)
        assertTrue(response.text.isNotEmpty())
        
        // 获取更新后的会话
        val updatedSession = semanticService.getSession(session.id)
        
        // 验证会话历史
        assertNotNull(updatedSession)
        assertEquals(2, updatedSession.history.size)
        assertEquals("user", updatedSession.history[0].type)
        assertEquals("system", updatedSession.history[1].type)
    }
    
    @Test
    fun `test message processing`() {
        // 处理消息
        val result = semanticService.processMessage("北京明天的天气如何？")
        
        // 验证处理结果
        assertNotNull(result)
        assertNotNull(result.intentResult.topIntent)
        assertEquals("天气查询", result.intentResult.topIntent?.name)
        
        // 验证实体
        assertTrue(result.entities.isNotEmpty())
        
        // 生成响应
        val response = semanticService.generateResponse(result)
        
        // 验证响应
        assertNotNull(response)
        assertTrue(response.text.isNotEmpty())
    }
    
    /**
     * 配置本体
     */
    private fun configureOntology() {
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
        semanticService.ontologyManager.registerOntology(weatherOntology)
    }
    
    /**
     * 配置意图
     */
    private fun configureIntents() {
        // 创建问候意图
        val greetingIntent = semanticService.intentRecognizer.createIntent(
            name = "问候",
            description = "用户问候"
        )
        
        // 添加问候意图模式
        semanticService.intentRecognizer.addIntentPattern(
            intentId = greetingIntent.id,
            type = "keyword",
            pattern = "你好,您好,早上好,下午好,晚上好,嗨,哈喽,hello,hi"
        )
        
        // 创建天气查询意图
        val weatherQueryIntent = semanticService.intentRecognizer.createIntent(
            name = "天气查询",
            description = "查询天气"
        )
        
        // 添加天气查询意图模式
        semanticService.intentRecognizer.addIntentPattern(
            intentId = weatherQueryIntent.id,
            type = "keyword",
            pattern = "天气,气温,温度,下雨,下雪,晴天,阴天,多云"
        )
        
        // 创建告别意图
        val farewellIntent = semanticService.intentRecognizer.createIntent(
            name = "告别",
            description = "用户告别"
        )
        
        // 添加告别意图模式
        semanticService.intentRecognizer.addIntentPattern(
            intentId = farewellIntent.id,
            type = "keyword",
            pattern = "再见,拜拜,拜,goodbye,bye,下次见,回头见"
        )
    }
    
    /**
     * 配置实体
     */
    private fun configureEntities() {
        // 创建位置实体类型
        val locationEntityType = semanticService.entityResolver.createEntityType(
            name = "位置",
            description = "位置实体"
        )
        
        // 创建位置字典提取器
        semanticService.entityResolver.createDictionaryExtractor(
            entityTypeId = locationEntityType.id,
            name = "城市提取器",
            entries = listOf("北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "重庆", "武汉", "西安")
        )
        
        // 创建时间实体类型
        val timeEntityType = semanticService.entityResolver.createEntityType(
            name = "时间",
            description = "时间实体"
        )
        
        // 创建时间字典提取器
        semanticService.entityResolver.createDictionaryExtractor(
            entityTypeId = timeEntityType.id,
            name = "时间提取器",
            entries = listOf("今天", "明天", "后天", "昨天", "前天", "上午", "下午", "晚上", "早上", "中午")
        )
        
        // 创建天气实体类型
        val weatherEntityType = semanticService.entityResolver.createEntityType(
            name = "天气",
            description = "天气实体"
        )
        
        // 创建天气字典提取器
        semanticService.entityResolver.createDictionaryExtractor(
            entityTypeId = weatherEntityType.id,
            name = "天气提取器",
            entries = listOf("晴天", "阴天", "多云", "下雨", "下雪", "雾", "霾", "冰雹", "台风", "龙卷风")
        )
    }
}
