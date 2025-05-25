package com.kastrax.ai2db

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTest {
    
    @Test
    fun contextLoads() {
        // 仅测试Spring上下文是否加载成功
    }
} 