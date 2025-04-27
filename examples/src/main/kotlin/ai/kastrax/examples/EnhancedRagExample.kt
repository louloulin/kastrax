package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.*
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.vectorstore.RagInMemoryVectorStore
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 增强型RAG示例，展示了如何使用各种文档加载器。
 */
fun main() = runBlocking {
    println("KastraX 增强型RAG示例")
    println("===================")
    
    // 创建向量存储和嵌入服务
    val vectorStore = RagInMemoryVectorStore()
    val embeddingService = RandomEmbeddingService(dimensions = 1536)
    
    // 创建RAG系统
    val rag = RAG(vectorStore, embeddingService)
    
    // 创建文档分割器
    val splitter = RecursiveCharacterTextSplitter(
        chunkSize = 500,
        chunkOverlap = 100
    )
    
    // 创建示例文件目录
    val examplesDir = File("examples_data")
    if (!examplesDir.exists()) {
        examplesDir.mkdirs()
    }
    
    // 创建示例文件
    createExampleFiles(examplesDir)
    
    // 加载CSV文件
    println("\n加载CSV文件...")
    val csvFile = File(examplesDir, "example.csv")
    val csvLoader = CsvDocumentLoader(csvFile)
    val csvDocuments = csvLoader.load()
    println("加载了 ${csvDocuments.size} 个CSV文档")
    
    // 加载JSON文件
    println("\n加载JSON文件...")
    val jsonFile = File(examplesDir, "example.json")
    val jsonLoader = JsonDocumentLoader(jsonFile)
    val jsonDocuments = jsonLoader.load()
    println("加载了 ${jsonDocuments.size} 个JSON文档")
    
    // 加载XML文件
    println("\n加载XML文件...")
    val xmlFile = File(examplesDir, "example.xml")
    val xmlLoader = XmlDocumentLoader(xmlFile)
    val xmlDocuments = xmlLoader.load()
    println("加载了 ${xmlDocuments.size} 个XML文档")
    
    // 加载Markdown文件
    println("\n加载Markdown文件...")
    val mdFile = File(examplesDir, "example.md")
    val mdLoader = MarkdownDocumentLoader(mdFile)
    val mdDocuments = mdLoader.load()
    println("加载了 ${mdDocuments.size} 个Markdown文档")
    
    // 将所有文档添加到RAG系统
    println("\n将文档添加到RAG系统...")
    rag.addDocuments(csvDocuments)
    rag.addDocuments(jsonDocuments)
    rag.addDocuments(xmlDocuments)
    rag.addDocuments(mdDocuments)
    
    println("总共添加了 ${csvDocuments.size + jsonDocuments.size + xmlDocuments.size + mdDocuments.size} 个文档")
    
    // 创建Agent
    val agent = agent {
        name = "RAG助手"
        instructions = """
            你是一个RAG助手，可以回答关于示例数据的问题。
            使用提供的上下文来回答问题。
            如果上下文中没有相关信息，请说明你不知道。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
    }
    
    // 示例查询
    val queries = listOf(
        "谁是John Doe？",
        "伦敦有哪些人？",
        "这些数据中最年长的人是谁？",
        "Markdown文档的标题是什么？"
    )
    
    // 处理查询
    println("\n处理查询...")
    for (query in queries) {
        println("\n查询: $query")
        
        // 生成上下文
        val context = rag.generateContext(query, limit = 3)
        println("上下文长度: ${context.length} 字符")
        
        // 使用Agent生成回答
        val response = agent.generate(
            """
            基于以下上下文回答问题:
            
            $context
            
            问题: $query
            """.trimIndent()
        )
        
        println("回答: ${response.text}")
    }
    
    println("\n示例完成")
}

/**
 * 创建示例文件。
 */
private fun createExampleFiles(dir: File) {
    // 创建CSV文件
    val csvFile = File(dir, "example.csv")
    csvFile.writeText("""
        Name,Age,City
        John Doe,30,New York
        Jane Smith,25,London
        Bob Johnson,40,Paris
        Alice Brown,35,London
        Charlie Wilson,45,Tokyo
    """.trimIndent())
    
    // 创建JSON文件
    val jsonFile = File(dir, "example.json")
    jsonFile.writeText("""
        {
            "people": [
                {
                    "name": "John Doe",
                    "age": 30,
                    "city": "New York",
                    "skills": ["Java", "Kotlin", "Python"]
                },
                {
                    "name": "Jane Smith",
                    "age": 25,
                    "city": "London",
                    "skills": ["JavaScript", "TypeScript", "React"]
                },
                {
                    "name": "Bob Johnson",
                    "age": 40,
                    "city": "Paris",
                    "skills": ["C++", "Rust", "Go"]
                }
            ],
            "companies": [
                {
                    "name": "Tech Corp",
                    "location": "New York",
                    "employees": 500
                },
                {
                    "name": "Data Systems",
                    "location": "London",
                    "employees": 200
                }
            ]
        }
    """.trimIndent())
    
    // 创建XML文件
    val xmlFile = File(dir, "example.xml")
    xmlFile.writeText("""
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
            <people>
                <person id="1">
                    <name>John Doe</name>
                    <age>30</age>
                    <city>New York</city>
                    <department>Engineering</department>
                </person>
                <person id="2">
                    <name>Jane Smith</name>
                    <age>25</age>
                    <city>London</city>
                    <department>Marketing</department>
                </person>
                <person id="3">
                    <name>Bob Johnson</name>
                    <age>40</age>
                    <city>Paris</city>
                    <department>Finance</department>
                </person>
            </people>
            <projects>
                <project id="A">
                    <name>Website Redesign</name>
                    <lead>Jane Smith</lead>
                </project>
                <project id="B">
                    <name>Mobile App</name>
                    <lead>John Doe</lead>
                </project>
            </projects>
        </data>
    """.trimIndent())
    
    // 创建Markdown文件
    val mdFile = File(dir, "example.md")
    mdFile.writeText("""
        ---
        title: 员工和项目信息
        author: KastraX团队
        date: 2023-06-15
        ---
        
        # 员工和项目信息
        
        这是一个包含员工和项目信息的文档。
        
        ## 员工
        
        1. **John Doe**
           - 年龄: 30
           - 城市: New York
           - 部门: Engineering
           - 技能: Java, Kotlin, Python
        
        2. **Jane Smith**
           - 年龄: 25
           - 城市: London
           - 部门: Marketing
           - 技能: JavaScript, TypeScript, React
        
        3. **Bob Johnson**
           - 年龄: 40
           - 城市: Paris
           - 部门: Finance
           - 技能: C++, Rust, Go
        
        ## 项目
        
        1. **Website Redesign**
           - 负责人: Jane Smith
           - 状态: 进行中
        
        2. **Mobile App**
           - 负责人: John Doe
           - 状态: 计划中
    """.trimIndent())
}
