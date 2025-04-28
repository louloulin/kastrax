package ai.kastrax.examples.mcp

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 知识库查询 MCP 应用案例
 * 
 * 这个示例展示了如何创建一个提供知识库查询功能的 MCP 服务器，
 * 以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。
 */
fun main() = runBlocking {
    println("启动知识库查询 MCP 应用案例...")
    
    // 启动服务器
    val serverJob = launch {
        val server = createKnowledgeBaseServer()
        server.startSSE(port = 8084)
        println("知识库查询 MCP 服务器已启动在端口 8084")
        
        // 保持服务器运行
        try {
            while (true) {
                delay(1000)
            }
        } finally {
            server.stop()
            println("知识库查询 MCP 服务器已停止")
        }
    }
    
    // 等待服务器启动
    delay(2000)
    
    // 创建并使用客户端
    val client = createKnowledgeBaseClient()
    
    try {
        // 连接到服务器
        println("连接到知识库查询 MCP 服务器...")
        client.connect()
        println("已连接到知识库查询 MCP 服务器")
        
        // 获取服务器能力
        val hasTools = client.supportsCapability("tools")
        println("服务器支持工具: $hasTools")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 获取知识库列表
        println("\n获取知识库列表...")
        val kbListResult = client.callTool("list_knowledge_bases", emptyMap())
        println("知识库列表: $kbListResult")
        
        // 查询不同主题的知识
        val queries = listOf(
            "人工智能" to "ai_kb",
            "机器学习" to "ai_kb",
            "深度学习" to "ai_kb",
            "Kotlin语言" to "programming_kb",
            "Java语言" to "programming_kb",
            "Python语言" to "programming_kb",
            "中国历史" to "history_kb",
            "世界历史" to "history_kb"
        )
        
        for ((query, kbId) in queries) {
            println("\n在知识库 $kbId 中查询: $query")
            val searchResult = client.callTool("search_knowledge", mapOf(
                "query" to query,
                "kb_id" to kbId,
                "max_results" to 3
            ))
            println("查询结果: $searchResult")
        }
        
        // 获取特定文档
        println("\n获取特定文档...")
        val documentResult = client.callTool("get_document", mapOf(
            "doc_id" to "ai_intro_doc",
            "kb_id" to "ai_kb"
        ))
        println("文档内容: $documentResult")
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与知识库查询 MCP 服务器的连接")
        
        // 停止服务器
        serverJob.cancel()
    }
}

/**
 * 创建知识库查询 MCP 服务器
 */
private fun createKnowledgeBaseServer() = mcpServer {
    name("KnowledgeBaseMCPServer")
    version("1.0.0")
    
    // 添加知识库列表工具
    tool {
        name = "list_knowledge_bases"
        description = "获取可用的知识库列表"
        
        // 设置执行函数
        handler { _ ->
            println("执行list_knowledge_bases工具")
            
            // 获取知识库列表
            val knowledgeBases = getKnowledgeBases()
            
            // 格式化返回结果
            val kbList = knowledgeBases.joinToString("\n\n") { kb ->
                """
                |ID: ${kb.id}
                |名称: ${kb.name}
                |描述: ${kb.description}
                |文档数量: ${kb.documentCount}
                |最后更新: ${kb.lastUpdated}
                """.trimMargin()
            }
            
            """
            |可用知识库列表:
            |
            |$kbList
            """.trimMargin()
        }
    }
    
    // 添加知识库搜索工具
    tool {
        name = "search_knowledge"
        description = "在指定知识库中搜索信息"
        
        // 添加参数
        parameters {
            parameter {
                name = "query"
                description = "搜索查询"
                type = "string"
                required = true
            }
            
            parameter {
                name = "kb_id"
                description = "知识库ID"
                type = "string"
                required = true
            }
            
            parameter {
                name = "max_results"
                description = "最大结果数量"
                type = "number"
                required = false
            }
        }
        
        // 设置执行函数
        handler { params ->
            val query = params["query"] as? String ?: ""
            val kbId = params["kb_id"] as? String ?: ""
            val maxResults = (params["max_results"] as? Number)?.toInt() ?: 5
            
            println("执行search_knowledge工具，查询: $query, 知识库ID: $kbId, 最大结果数: $maxResults")
            
            // 搜索知识库
            val searchResults = searchKnowledgeBase(query, kbId, maxResults)
            
            // 如果没有结果
            if (searchResults.isEmpty()) {
                return@handler "未找到与 '$query' 相关的信息。"
            }
            
            // 格式化返回结果
            val resultsList = searchResults.joinToString("\n\n") { result ->
                """
                |标题: ${result.title}
                |相关度: ${result.relevance}%
                |摘要: ${result.summary}
                |文档ID: ${result.documentId}
                """.trimMargin()
            }
            
            """
            |查询: $query
            |知识库: ${getKnowledgeBaseName(kbId)}
            |找到 ${searchResults.size} 个结果:
            |
            |$resultsList
            """.trimMargin()
        }
    }
    
    // 添加获取文档工具
    tool {
        name = "get_document"
        description = "获取指定文档的内容"
        
        // 添加参数
        parameters {
            parameter {
                name = "doc_id"
                description = "文档ID"
                type = "string"
                required = true
            }
            
            parameter {
                name = "kb_id"
                description = "知识库ID"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val docId = params["doc_id"] as? String ?: ""
            val kbId = params["kb_id"] as? String ?: ""
            
            println("执行get_document工具，文档ID: $docId, 知识库ID: $kbId")
            
            // 获取文档
            val document = getDocument(docId, kbId)
            
            // 如果文档不存在
            if (document == null) {
                return@handler "未找到文档 '$docId'。"
            }
            
            // 格式化返回结果
            """
            |标题: ${document.title}
            |作者: ${document.author}
            |创建日期: ${document.createdDate}
            |最后更新: ${document.lastUpdated}
            |知识库: ${getKnowledgeBaseName(kbId)}
            |
            |内容:
            |${document.content}
            """.trimMargin()
        }
    }
}

/**
 * 创建知识库查询 MCP 客户端
 */
private fun createKnowledgeBaseClient() = mcpClient {
    name("KnowledgeBaseMCPClient")
    version("1.0.0")
    
    server {
        sse {
            url = "http://localhost:8084"
        }
    }
}

/**
 * 知识库数据类
 */
private data class KnowledgeBase(
    val id: String,
    val name: String,
    val description: String,
    val documentCount: Int,
    val lastUpdated: String
)

/**
 * 搜索结果数据类
 */
private data class SearchResult(
    val title: String,
    val summary: String,
    val relevance: Int,
    val documentId: String
)

/**
 * 文档数据类
 */
private data class Document(
    val id: String,
    val title: String,
    val author: String,
    val content: String,
    val createdDate: String,
    val lastUpdated: String
)

/**
 * 获取知识库列表
 */
private fun getKnowledgeBases(): List<KnowledgeBase> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val now = LocalDateTime.now().format(formatter)
    
    return listOf(
        KnowledgeBase(
            id = "ai_kb",
            name = "人工智能知识库",
            description = "包含人工智能、机器学习、深度学习等相关知识",
            documentCount = 150,
            lastUpdated = now
        ),
        KnowledgeBase(
            id = "programming_kb",
            name = "编程语言知识库",
            description = "包含各种编程语言的教程、文档和最佳实践",
            documentCount = 320,
            lastUpdated = now
        ),
        KnowledgeBase(
            id = "history_kb",
            name = "历史知识库",
            description = "包含中国历史和世界历史的重要事件、人物和时期",
            documentCount = 280,
            lastUpdated = now
        ),
        KnowledgeBase(
            id = "science_kb",
            name = "科学知识库",
            description = "包含物理、化学、生物等自然科学领域的知识",
            documentCount = 210,
            lastUpdated = now
        )
    )
}

/**
 * 获取知识库名称
 */
private fun getKnowledgeBaseName(kbId: String): String {
    return when (kbId) {
        "ai_kb" -> "人工智能知识库"
        "programming_kb" -> "编程语言知识库"
        "history_kb" -> "历史知识库"
        "science_kb" -> "科学知识库"
        else -> "未知知识库"
    }
}

/**
 * 搜索知识库
 */
private fun searchKnowledgeBase(query: String, kbId: String, maxResults: Int): List<SearchResult> {
    // 这里只是模拟搜索，实际应用中应该使用真实的搜索引擎
    val results = when (kbId) {
        "ai_kb" -> searchAIKnowledgeBase(query)
        "programming_kb" -> searchProgrammingKnowledgeBase(query)
        "history_kb" -> searchHistoryKnowledgeBase(query)
        "science_kb" -> searchScienceKnowledgeBase(query)
        else -> emptyList()
    }
    
    return results.take(maxResults)
}

/**
 * 搜索人工智能知识库
 */
private fun searchAIKnowledgeBase(query: String): List<SearchResult> {
    val aiTopics = mapOf(
        "人工智能" to listOf(
            SearchResult(
                title = "人工智能简介",
                summary = "人工智能（AI）是计算机科学的一个分支，致力于创建能够模拟人类智能的系统。",
                relevance = 95,
                documentId = "ai_intro_doc"
            ),
            SearchResult(
                title = "人工智能的历史",
                summary = "人工智能的发展可以追溯到20世纪50年代，经历了多次起伏。",
                relevance = 90,
                documentId = "ai_history_doc"
            ),
            SearchResult(
                title = "人工智能的应用领域",
                summary = "人工智能已经在医疗、金融、教育等多个领域得到广泛应用。",
                relevance = 85,
                documentId = "ai_applications_doc"
            )
        ),
        "机器学习" to listOf(
            SearchResult(
                title = "机器学习基础",
                summary = "机器学习是人工智能的一个子领域，专注于开发能够从数据中学习的算法。",
                relevance = 95,
                documentId = "ml_basics_doc"
            ),
            SearchResult(
                title = "监督学习与无监督学习",
                summary = "机器学习主要分为监督学习、无监督学习和强化学习三种类型。",
                relevance = 90,
                documentId = "ml_types_doc"
            ),
            SearchResult(
                title = "常见机器学习算法",
                summary = "决策树、随机森林、支持向量机等是常用的机器学习算法。",
                relevance = 85,
                documentId = "ml_algorithms_doc"
            )
        ),
        "深度学习" to listOf(
            SearchResult(
                title = "深度学习入门",
                summary = "深度学习是机器学习的一个分支，使用多层神经网络处理复杂问题。",
                relevance = 95,
                documentId = "dl_intro_doc"
            ),
            SearchResult(
                title = "神经网络架构",
                summary = "卷积神经网络（CNN）、循环神经网络（RNN）和变换器（Transformer）是常见的神经网络架构。",
                relevance = 90,
                documentId = "dl_architectures_doc"
            ),
            SearchResult(
                title = "深度学习框架比较",
                summary = "TensorFlow、PyTorch和Keras是目前最流行的深度学习框架。",
                relevance = 85,
                documentId = "dl_frameworks_doc"
            )
        )
    )
    
    // 根据查询返回相关结果
    return aiTopics.entries.find { (key, _) -> 
        query.contains(key) 
    }?.value ?: emptyList()
}

/**
 * 搜索编程语言知识库
 */
private fun searchProgrammingKnowledgeBase(query: String): List<SearchResult> {
    val programmingTopics = mapOf(
        "Kotlin" to listOf(
            SearchResult(
                title = "Kotlin语言简介",
                summary = "Kotlin是一种现代的、静态类型的编程语言，可以运行在JVM上。",
                relevance = 95,
                documentId = "kotlin_intro_doc"
            ),
            SearchResult(
                title = "Kotlin协程",
                summary = "Kotlin协程提供了一种简洁、优雅的方式来处理异步编程。",
                relevance = 90,
                documentId = "kotlin_coroutines_doc"
            ),
            SearchResult(
                title = "Kotlin与Java比较",
                summary = "Kotlin相比Java有更简洁的语法、更好的空安全和函数式编程支持。",
                relevance = 85,
                documentId = "kotlin_vs_java_doc"
            )
        ),
        "Java" to listOf(
            SearchResult(
                title = "Java语言基础",
                summary = "Java是一种广泛使用的面向对象编程语言，具有'一次编写，到处运行'的特性。",
                relevance = 95,
                documentId = "java_basics_doc"
            ),
            SearchResult(
                title = "Java集合框架",
                summary = "Java集合框架提供了一系列类和接口，用于存储和操作对象组。",
                relevance = 90,
                documentId = "java_collections_doc"
            ),
            SearchResult(
                title = "Java多线程编程",
                summary = "Java提供了内置的多线程支持，允许开发者创建并发应用程序。",
                relevance = 85,
                documentId = "java_multithreading_doc"
            )
        ),
        "Python" to listOf(
            SearchResult(
                title = "Python入门指南",
                summary = "Python是一种高级、解释型、通用编程语言，以简洁易读的语法著称。",
                relevance = 95,
                documentId = "python_intro_doc"
            ),
            SearchResult(
                title = "Python数据科学库",
                summary = "NumPy、Pandas和Matplotlib是Python中常用的数据科学库。",
                relevance = 90,
                documentId = "python_data_science_doc"
            ),
            SearchResult(
                title = "Python Web框架",
                summary = "Django和Flask是Python中最流行的Web开发框架。",
                relevance = 85,
                documentId = "python_web_frameworks_doc"
            )
        )
    )
    
    // 根据查询返回相关结果
    for ((key, results) in programmingTopics) {
        if (query.contains(key, ignoreCase = true)) {
            return results
        }
    }
    
    return emptyList()
}

/**
 * 搜索历史知识库
 */
private fun searchHistoryKnowledgeBase(query: String): List<SearchResult> {
    val historyTopics = mapOf(
        "中国历史" to listOf(
            SearchResult(
                title = "中国古代历史概览",
                summary = "中国有着五千多年的文明历史，经历了多个朝代的更替。",
                relevance = 95,
                documentId = "chinese_history_overview_doc"
            ),
            SearchResult(
                title = "中国四大发明",
                summary = "造纸术、印刷术、火药和指南针被称为中国古代四大发明。",
                relevance = 90,
                documentId = "chinese_inventions_doc"
            ),
            SearchResult(
                title = "中国历代王朝",
                summary = "从夏朝到清朝，中国历史上共有多个重要王朝。",
                relevance = 85,
                documentId = "chinese_dynasties_doc"
            )
        ),
        "世界历史" to listOf(
            SearchResult(
                title = "世界历史重要事件",
                summary = "从古代文明到现代社会，世界历史上发生了许多改变人类进程的重要事件。",
                relevance = 95,
                documentId = "world_history_events_doc"
            ),
            SearchResult(
                title = "工业革命",
                summary = "工业革命始于18世纪的英国，彻底改变了人类的生产和生活方式。",
                relevance = 90,
                documentId = "industrial_revolution_doc"
            ),
            SearchResult(
                title = "两次世界大战",
                summary = "第一次和第二次世界大战是20世纪最具破坏性的全球性冲突。",
                relevance = 85,
                documentId = "world_wars_doc"
            )
        )
    )
    
    // 根据查询返回相关结果
    for ((key, results) in historyTopics) {
        if (query.contains(key)) {
            return results
        }
    }
    
    return emptyList()
}

/**
 * 搜索科学知识库
 */
private fun searchScienceKnowledgeBase(query: String): List<SearchResult> {
    // 简化实现，返回空列表
    return emptyList()
}

/**
 * 获取文档
 */
private fun getDocument(docId: String, kbId: String): Document? {
    // 这里只是模拟文档获取，实际应用中应该从数据库或文件系统中获取
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val now = LocalDateTime.now().format(formatter)
    
    return when (docId) {
        "ai_intro_doc" -> Document(
            id = docId,
            title = "人工智能简介",
            author = "张三",
            content = """
                人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，致力于创建能够模拟人类智能的系统。
                
                人工智能研究的主要目标包括：
                1. 推理和问题解决
                2. 知识表示
                3. 规划
                4. 学习
                5. 自然语言处理
                6. 感知
                7. 移动和操作物体的能力
                
                人工智能可以分为弱人工智能和强人工智能。弱人工智能专注于解决特定问题，而强人工智能则旨在创建具有与人类相当或超越人类的通用智能的系统。
                
                目前，人工智能已经在医疗诊断、自动驾驶、语音助手、推荐系统等多个领域取得了显著成果。随着技术的不断发展，人工智能将在未来发挥越来越重要的作用。
            """.trimIndent(),
            createdDate = "2023-01-15",
            lastUpdated = now
        )
        "kotlin_intro_doc" -> Document(
            id = docId,
            title = "Kotlin语言简介",
            author = "李四",
            content = """
                Kotlin是一种现代的、静态类型的编程语言，由JetBrains开发，可以运行在JVM上。Kotlin于2011年首次发布，2017年被Google宣布为Android开发的官方支持语言。
                
                Kotlin的主要特点包括：
                1. 简洁：Kotlin减少了大量的样板代码
                2. 安全：Kotlin提供了空安全和不可变性等特性
                3. 互操作性：Kotlin可以与Java代码无缝互操作
                4. 函数式编程：Kotlin支持高阶函数、lambda表达式等函数式编程特性
                
                Kotlin可以用于开发各种应用，包括：
                - Android应用
                - 服务器端应用
                - Web前端（通过Kotlin/JS）
                - 原生应用（通过Kotlin/Native）
                
                Kotlin的语法简洁而强大，例如：
                ```kotlin
                // 定义一个不可变变量
                val name = "Kotlin"
                
                // 定义一个可变变量
                var age = 10
                
                // 函数定义
                fun greet(name: String): String {
                    return "Hello, $name!"
                }
                
                // 使用lambda表达式
                val numbers = listOf(1, 2, 3, 4, 5)
                val doubled = numbers.map { it * 2 }
                ```
                
                Kotlin正在迅速获得开发者的青睐，成为Java之外的一个重要选择。
            """.trimIndent(),
            createdDate = "2023-02-20",
            lastUpdated = now
        )
        else -> null
    }
}
