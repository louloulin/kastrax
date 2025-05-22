package com.kastrax.ai2db.connection.model

import java.time.Instant

/**
 * Status of a database connection
 */
data class ConnectionStatus(
    val isConnected: Boolean,
    val message: String,
    val responseTimeMs: Long,
    val timestamp: Instant = Instant.now(),
    val databaseVersion: String? = null,
    val serverInfo: Map<String, String> = mapOf()
) 