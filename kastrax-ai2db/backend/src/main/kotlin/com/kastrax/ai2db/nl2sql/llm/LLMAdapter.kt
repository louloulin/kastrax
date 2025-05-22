package com.kastrax.ai2db.nl2sql.llm

/**
 * Interface for interacting with language models
 */
interface LLMAdapter {
    /**
     * Complete a prompt
     */
    suspend fun complete(prompt: String): String
    
    /**
     * Complete a prompt with a response parser
     */
    suspend fun <T> complete(prompt: String, parser: ResponseParser<T>): T
    
    /**
     * Generate embeddings for a text
     */
    suspend fun embed(text: String): List<Float>
}

/**
 * Interface for parsing LLM responses
 */
interface ResponseParser<T> {
    /**
     * Parse a response from the LLM
     */
    fun parse(response: String): T
} 