package com.kastrax.ai2db

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import com.kastrax.ai2db.connection.connector.DatabaseConnector
import com.kastrax.ai2db.connection.connector.MySQLConnector

@TestConfiguration
class TestConfig {
    
    @Bean
    @Primary
    fun databaseConnector(): DatabaseConnector {
        return MySQLConnector()
    }
} 