package com.kastrax.ai2db.nl2sql.controller

import com.kastrax.ai2db.nl2sql.agent.NL2SQLAgent
import com.kastrax.ai2db.nl2sql.model.*
import com.kastrax.core.agent.AgentResult
import com.kastrax.memory.Memory
import com.kastrax.memory.model.Message
import com.kastrax.memory.model.MessageRole
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebMvcTest(NL2SQLController::class)
@ContextConfiguration(classes = [NL2SQLController::class])
class NL2SQLControllerTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockBean
    private lateinit var nl2sqlAgent: NL2SQLAgent
    
    @MockBean
    private lateinit var memory: Memory
    
    @Autowired
    private lateinit var objectMapper: ObjectMapper
    
    @BeforeEach
    fun setUp() {
        // Setup is handled by Spring Boot Test annotations
    }
    
    @Test
    fun `should convert natural language to SQL successfully`() {
        runTest {
            // Given
            val request = NL2SQLRequest(
                query = "查询所有年龄大于18岁的用户",
                connectionId = "test-connection",
                sessionId = "test-session"
            )
            
            val sqlResult = SQLGenerationResult(
                sql = "SELECT * FROM users WHERE age > 18",
                confidence = 0.95,
                queryType = QueryType.SELECT,
                complexity = QueryComplexity.LOW,
                explanation = "查询年龄大于18岁的所有用户记录",
                steps = listOf(
                    ConversionStep(
                        step = "分析查询意图",
                        description = "识别为查询操作",
                        confidence = 0.9
                    )
                )
            )
            
            val agentResult = AgentResult.success(sqlResult)
            
            coEvery { nl2sqlAgent.generate(any(), any()) } returns agentResult
            
            // When & Then
            mockMvc.perform(
                post("/api/nl2sql/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sql").value("SELECT * FROM users WHERE age > 18"))
                .andExpect(jsonPath("$.data.confidence").value(0.95))
                .andExpect(jsonPath("$.data.queryType").value("SELECT"))
                .andExpect(jsonPath("$.data.complexity").value("LOW"))
            
            coVerify { nl2sqlAgent.generate(request.query, match { it["connectionId"] == "test-connection" }) }
        }
    }
    
    @Test
    fun `should handle conversion failure gracefully`() {
        runTest {
            // Given
            val request = NL2SQLRequest(
                query = "无效的查询请求",
                connectionId = "test-connection",
                sessionId = "test-session"
            )
            
            val agentResult = AgentResult.failure<SQLGenerationResult>("无法理解查询意图")
            
            coEvery { nl2sqlAgent.generate(any(), any()) } returns agentResult
            
            // When & Then
            mockMvc.perform(
                post("/api/nl2sql/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("无法理解查询意图"))
        }
    }
    
    @Test
    fun `should handle batch conversion successfully`() {
        runTest {
            // Given
            val request = BatchNL2SQLRequest(
                queries = listOf(
                    "查询所有用户",
                    "查询订单总数"
                ),
                connectionId = "test-connection",
                sessionId = "test-session"
            )
            
            val sqlResult1 = SQLGenerationResult(
                sql = "SELECT * FROM users",
                confidence = 0.9,
                queryType = QueryType.SELECT,
                complexity = QueryComplexity.LOW,
                explanation = "查询所有用户记录",
                steps = emptyList()
            )
            
            val sqlResult2 = SQLGenerationResult(
                sql = "SELECT COUNT(*) FROM orders",
                confidence = 0.85,
                queryType = QueryType.SELECT,
                complexity = QueryComplexity.LOW,
                explanation = "统计订单总数",
                steps = emptyList()
            )
            
            coEvery { nl2sqlAgent.generate("查询所有用户", any()) } returns AgentResult.success(sqlResult1)
            coEvery { nl2sqlAgent.generate("查询订单总数", any()) } returns AgentResult.success(sqlResult2)
            
            // When & Then
            mockMvc.perform(
                post("/api/nl2sql/batch-convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sql").value("SELECT * FROM users"))
                .andExpect(jsonPath("$.data[1].sql").value("SELECT COUNT(*) FROM orders"))
        }
    }
    
    @Test
    fun `should handle partial batch conversion failure`() {
        runTest {
            // Given
            val request = BatchNL2SQLRequest(
                queries = listOf(
                    "查询所有用户",
                    "无效查询"
                ),
                connectionId = "test-connection",
                sessionId = "test-session"
            )
            
            val sqlResult1 = SQLGenerationResult(
                sql = "SELECT * FROM users",
                confidence = 0.9,
                queryType = QueryType.SELECT,
                complexity = QueryComplexity.LOW,
                explanation = "查询所有用户记录",
                steps = emptyList()
            )
            
            coEvery { nl2sqlAgent.generate("查询所有用户", any()) } returns AgentResult.success(sqlResult1)
            coEvery { nl2sqlAgent.generate("无效查询", any()) } returns AgentResult.failure("无法理解查询意图")
            
            // When & Then
            mockMvc.perform(
                post("/api/nl2sql/batch-convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sql").value("SELECT * FROM users"))
                .andExpect(jsonPath("$.data[1]").doesNotExist())
        }
    }
    
    @Test
    fun `should retrieve conversation history successfully`() {
        runTest {
            // Given
            val sessionId = "test-session"
            val messages = listOf(
                Message(
                    id = "msg1",
                    sessionId = sessionId,
                    role = MessageRole.USER,
                    content = "查询所有用户",
                    timestamp = System.currentTimeMillis()
                ),
                Message(
                    id = "msg2",
                    sessionId = sessionId,
                    role = MessageRole.ASSISTANT,
                    content = "SELECT * FROM users",
                    timestamp = System.currentTimeMillis()
                )
            )
            
            coEvery { memory.getMessages(sessionId, any()) } returns messages
            
            // When & Then
            mockMvc.perform(
                get("/api/nl2sql/history/{sessionId}", sessionId)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].content").value("查询所有用户"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[1].content").value("SELECT * FROM users"))
        }
    }
    
    @Test
    fun `should handle history retrieval with limit parameter`() {
        runTest {
            // Given
            val sessionId = "test-session"
            val limit = 5
            
            coEvery { memory.getMessages(sessionId, limit) } returns emptyList()
            
            // When & Then
            mockMvc.perform(
                get("/api/nl2sql/history/{sessionId}", sessionId)
                    .param("limit", limit.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
            
            coVerify { memory.getMessages(sessionId, limit) }
        }
    }
    
    @Test
    fun `should clear conversation history successfully`() {
        runTest {
            // Given
            val sessionId = "test-session"
            
            coEvery { memory.clearSession(sessionId) } returns Unit
            
            // When & Then
            mockMvc.perform(
                delete("/api/nl2sql/history/{sessionId}", sessionId)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("会话历史已清除"))
            
            coVerify { memory.clearSession(sessionId) }
        }
    }
    
    @Test
    fun `should handle history clearing failure`() {
        runTest {
            // Given
            val sessionId = "test-session"
            
            coEvery { memory.clearSession(sessionId) } throws RuntimeException("清除失败")
            
            // When & Then
            mockMvc.perform(
                delete("/api/nl2sql/history/{sessionId}", sessionId)
            )
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("清除失败"))
        }
    }
    
    @Test
    fun `should return health check successfully`() {
        // When & Then
        mockMvc.perform(
            get("/api/nl2sql/health")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("healthy"))
            .andExpect(jsonPath("$.service").value("NL2SQL"))
            .andExpect(jsonPath("$.timestamp").exists())
    }
    
    @Test
    fun `should validate request parameters for conversion`() {
        // Given - Request with missing query
        val invalidRequest = mapOf(
            "connectionId" to "test-connection",
            "sessionId" to "test-session"
        )
        
        // When & Then
        mockMvc.perform(
            post("/api/nl2sql/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `should validate request parameters for batch conversion`() {
        // Given - Request with empty queries
        val invalidRequest = BatchNL2SQLRequest(
            queries = emptyList(),
            connectionId = "test-connection",
            sessionId = "test-session"
        )
        
        // When & Then
        mockMvc.perform(
            post("/api/nl2sql/batch-convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `should handle malformed JSON request`() {
        // When & Then
        mockMvc.perform(
            post("/api/nl2sql/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }")
        )
            .andExpect(status().isBadRequest)
    }
    
    @Test
    fun `should handle agent execution timeout`() {
        runTest {
            // Given
            val request = NL2SQLRequest(
                query = "复杂查询",
                connectionId = "test-connection",
                sessionId = "test-session"
            )
            
            coEvery { nl2sqlAgent.generate(any(), any()) } throws RuntimeException("执行超时")
            
            // When & Then
            mockMvc.perform(
                post("/api/nl2sql/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("执行超时"))
        }
    }
}