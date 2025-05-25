package com.kastrax.ai2db.nl2sql.model

/**
 * Context for a conversation with the NL2SQL engine
 */
data class ConversationContext(
    val referencedTables: MutableSet<String> = mutableSetOf(),
    val referencedColumns: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val lastQueryType: QueryType? = null,
    val parameters: MutableMap<String, Any> = mutableMapOf(),
    val history: MutableList<ChatMessage> = mutableListOf()
) {
    /**
     * Add a message to the conversation history
     */
    fun addMessage(message: ChatMessage) {
        history.add(message)
    }
    
    /**
     * Add a referenced table
     */
    fun addTable(tableName: String) {
        referencedTables.add(tableName)
    }
    
    /**
     * Add a referenced column
     */
    fun addColumn(tableName: String, columnName: String) {
        if (!referencedColumns.containsKey(tableName)) {
            referencedColumns[tableName] = mutableSetOf()
        }
        referencedColumns[tableName]?.add(columnName)
    }
}

/**
 * A message in a conversation
 */
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Role of a message sender
 */
enum class MessageRole {
    USER,
    SYSTEM,
    ASSISTANT
} 