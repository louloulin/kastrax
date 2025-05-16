package ai.kastrax.code.service

import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConversationServiceTest {
    
    private lateinit var project: Project
    private lateinit var conversationService: ConversationService
    
    @Before
    fun setup() {
        project = mockk()
        conversationService = ConversationService(project)
    }
    
    @Test
    fun `test create conversation`() {
        // Act
        val conversation = conversationService.createConversation("Test Conversation")
        
        // Assert
        assertEquals("Test Conversation", conversation.title)
        assertTrue(conversation.messages.isEmpty())
    }
    
    @Test
    fun `test get current conversation`() {
        // Arrange
        val conversation = conversationService.createConversation("Test Conversation")
        
        // Act
        val currentConversation = conversationService.getCurrentConversation()
        
        // Assert
        assertEquals(conversation.id, currentConversation.id)
    }
    
    @Test
    fun `test add message to current conversation`() {
        // Arrange
        conversationService.createConversation("Test Conversation")
        val message = ChatMessage(
            role = MessageRole.USER,
            content = "Test message"
        )
        
        // Act
        conversationService.addMessageToCurrentConversation(message)
        
        // Assert
        val conversation = conversationService.getCurrentConversation()
        assertEquals(1, conversation.messages.size)
        assertEquals("Test message", conversation.messages[0].content)
    }
    
    @Test
    fun `test get sorted conversations`() {
        // Arrange
        val conversation1 = conversationService.createConversation("Conversation 1")
        val conversation2 = conversationService.createConversation("Conversation 2")
        
        // Add a message to conversation1 to update its timestamp
        Thread.sleep(10) // Ensure different timestamps
        conversation1.addMessage(ChatMessage(role = MessageRole.USER, content = "Test"))
        
        // Act
        val sortedConversations = conversationService.getSortedConversations()
        
        // Assert
        assertEquals(2, sortedConversations.size)
        assertEquals(conversation1.id, sortedConversations[0].id) // conversation1 should be first due to later update
    }
    
    @Test
    fun `test delete conversation`() {
        // Arrange
        val conversation = conversationService.createConversation("Test Conversation")
        
        // Act
        conversationService.deleteConversation(conversation.id)
        
        // Assert
        assertTrue(conversationService.getAllConversations().isEmpty())
    }
    
    @Test
    fun `test clear all conversations`() {
        // Arrange
        conversationService.createConversation("Conversation 1")
        conversationService.createConversation("Conversation 2")
        
        // Act
        conversationService.clearAllConversations()
        
        // Assert
        assertTrue(conversationService.getAllConversations().isEmpty())
    }
}
