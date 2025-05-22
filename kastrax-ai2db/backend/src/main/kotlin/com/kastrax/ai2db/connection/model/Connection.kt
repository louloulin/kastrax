package com.kastrax.ai2db.connection.model

import java.time.Instant

/**
 * Represents an active database connection
 */
data class Connection(
    val id: String,
    val config: ConnectionConfig,
    val createdAt: Instant = Instant.now(),
    val rawConnection: Any? = null // The actual native connection (JDBC Connection, MongoDB Client, etc.)
) 