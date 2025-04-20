# RAG 系统快速入门指南

本指南将帮助您快速上手 kastrax 框架的 RAG（检索增强生成）系统。RAG 系统允许您从各种来源加载文档，将其转换为向量表示，并在生成回答时检索相关信息，从而提高生成内容的准确性和相关性。

## 安装

首先，确保您已经添加了 kastrax-rag 模块的依赖：

```kotlin
// build.gradle.kts
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-rag:0.1.0")
}
```

## 基本用法

### 创建 RAG 系统

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.embedding.OpenAIEmbeddingService
import ai.kastrax.rag.embedding.FastEmbedEmbeddingService
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.djl.Device

// 创建向量存储
val vectorStore = InMemoryVectorStore()

// 选项 1：使用 OpenAI 嵌入服务（需要 API 密钥）
val openAIEmbeddingService = OpenAIEmbeddingService(apiKey = "your-api-key")
val ragWithOpenAI = RAG(vectorStore, openAIEmbeddingService)

// 选项 2：使用 FastEmbed 嵌入服务（本地模型，无需 API 密钥）
val fastEmbedService = FastEmbedEmbeddingService(
    modelId = "BAAI/bge-small-zh-v1.5",  // 中文小型模型
    maxLength = 512,
    device = Device.cpu()
)
val ragWithFastEmbed = RAG(vectorStore, fastEmbedService)

// 选项 3：使用随机嵌入服务（用于测试）
val randomEmbeddingService = RandomEmbeddingService(dimensions = 1536)
val ragWithRandom = RAG(vectorStore, randomEmbeddingService)
```

### 加载文档

```kotlin
import ai.kastrax.rag.document.TextFileDocumentLoader
import ai.kastrax.rag.document.WebPageDocumentLoader
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import java.io.File

// 创建文档分割器
val splitter = RecursiveCharacterTextSplitter(
    chunkSize = 1000,
    chunkOverlap = 200
)

// 从文本文件加载文档
val fileLoader = TextFileDocumentLoader("path/to/document.txt")
rag.loadDocuments(fileLoader, splitter)

// 从网页加载文档
val webLoader = WebPageDocumentLoader("https://example.com")
rag.loadDocuments(webLoader, splitter)

// 从目录加载文档
val directoryLoader = DirectoryDocumentLoader(
    directory = File("path/to/documents"),
    recursive = true,
    fileExtensions = listOf("txt", "md", "html")
)
rag.loadDocuments(directoryLoader, splitter)
```

### 搜索相关文档

```kotlin
// 搜索相关文档
val results = rag.search(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5
)

// 处理搜索结果
for (result in results) {
    println("文档: ${result.document.content}")
    println("相似度: ${result.score}")
    println("元数据: ${result.document.metadata}")
    println()
}
```

### 生成增强上下文

```kotlin
// 生成增强上下文
val context = rag.generateContext(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5
)

// 生成带元数据的增强上下文
val contextWithMetadata = rag.generateContextWithMetadata(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5,
    includeMetadata = true,
    metadataKeys = listOf("title", "source")
)
```

## 与代理集成

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.OpenAIProvider

// 创建 OpenAI 提供者
val openai = OpenAIProvider(
    apiKey = "your-api-key",
    model = "gpt-3.5-turbo"
)

// 创建 RAG 代理
val ragAgent = agent {
    name = "RAG Agent"
    instructions = """
        你是一个基于检索增强生成 (RAG) 的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

        请遵循以下准则：
        1. 仅使用提供的上下文信息回答问题
        2. 如果上下文中没有足够的信息，请坦诚地说明你不知道
        3. 不要编造信息或使用你自己的知识
        4. 引用信息的来源（如果有）
        5. 保持回答简洁、准确和有帮助

        上下文信息：
        {{context}}

        用户问题：
        {{question}}
    """.trimIndent()
    model = openai
}

// 回答问题
val question = "人工智能在医疗领域有哪些应用？"
val context = rag.generateContextWithMetadata(question)
val prompt = ragAgent.instructions
    .replace("{{context}}", context)
    .replace("{{question}}", question)
val answer = ragAgent.generate(prompt).text
```

## 与工作流集成

```kotlin
import ai.kastrax.core.workflow.workflow

// 创建研究工作流
val researchWorkflow = workflow {
    name = "research-workflow"
    description = "研究和报告生成工作流"

    step(researchAgent) {
        id = "research"
        name = "研究"
        description = "从上下文中提取相关信息"
        variables = mapOf(
            "context" to variable("$.input.context"),
            "question" to variable("$.input.question")
        )
    }

    step(analysisAgent) {
        id = "analysis"
        name = "分析"
        description = "分析研究结果"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text"),
            "question" to variable("$.input.question")
        )
    }

    step(reportAgent) {
        id = "report"
        name = "报告生成"
        description = "生成最终报告"
        after("research", "analysis")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text"),
            "analysis" to variable("$.steps.analysis.output.text"),
            "question" to variable("$.input.question")
        )
    }
}

// 检索相关上下文
val context = rag.generateContextWithMetadata(
    query = "人工智能的应用",
    limit = 10,
    minScore = 0.5
)

// 准备工作流输入
val input = mapOf(
    "context" to context,
    "question" to "人工智能在医疗领域有哪些应用？"
)

// 执行工作流
val result = researchWorkflow.execute(input)
```

## 完整示例

以下是一个完整的示例，展示了如何使用 RAG 系统创建一个简单的问答应用：

### 使用 OpenAI 嵌入服务的示例

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.embedding.OpenAIEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.kastrax.integrations.openai.OpenAIProvider
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    // 创建向量存储和嵌入服务
    val vectorStore = InMemoryVectorStore()
    val embeddingService = OpenAIEmbeddingService(apiKey = System.getenv("OPENAI_API_KEY"))

    // 创建 RAG 系统
    val rag = RAG(vectorStore, embeddingService)

    // 创建文档分割器
    val splitter = RecursiveCharacterTextSplitter(
        chunkSize = 500,
        chunkOverlap = 100
    )

    // 从目录加载文档
    val docsDir = File("docs")
    if (docsDir.exists() && docsDir.isDirectory) {
        val directoryLoader = DirectoryDocumentLoader(
            directory = docsDir,
            recursive = true,
            fileExtensions = listOf("txt", "md", "html")
        )
        rag.loadDocuments(directoryLoader, splitter)
    }

    // 创建 OpenAI 提供者
    val openai = OpenAIProvider(
        apiKey = System.getenv("OPENAI_API_KEY"),
        model = "gpt-3.5-turbo"
    )

    // 创建 RAG 代理
    val ragAgent = agent {
        name = "RAG Agent"
        instructions = """
            你是一个基于检索增强生成 (RAG) 的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

            上下文信息：
            {{context}}

            用户问题：
            {{question}}
        """.trimIndent()
        model = openai
    }

    // 使用 RAG 系统回答问题
    while (true) {
        print("\n请输入问题（输入 'exit' 退出）: ")
        val question = readLine() ?: ""

        if (question.equals("exit", ignoreCase = true)) {
            break
        }

        // 检索相关上下文
        val context = rag.generateContextWithMetadata(
            query = question,
            limit = 5,
            minScore = 0.5,
            includeMetadata = true
        )

        if (context.isEmpty()) {
            println("没有找到相关信息。")
            continue
        }

        // 构建提示
        val prompt = ragAgent.instructions
            .replace("{{context}}", context)
            .replace("{{question}}", question)

        // 生成回答
        println("\n正在生成回答...")
        val response = ragAgent.generate(prompt)

        // 显示回答
        println("\n回答:")
        println(response.text)
    }
}
```

### 使用 FastEmbed 嵌入服务的示例

以下是一个使用 FastEmbed 嵌入服务的示例，它使用本地模型生成嵌入向量，无需外部 API：

```kotlin
import ai.djl.Device
import ai.kastrax.core.agent.agent
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.document.TextFileDocumentLoader
import ai.kastrax.rag.embedding.FastEmbedEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.kastrax.integrations.openai.OpenAIProvider
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    // 创建 FastEmbed 嵌入服务
    val embeddingService = FastEmbedEmbeddingService(
        modelId = "BAAI/bge-small-zh-v1.5",  // 中文小型模型
        maxLength = 512,
        device = Device.cpu()
    )

    try {
        // 创建向量存储和 RAG 系统
        val vectorStore = InMemoryVectorStore()
        val rag = RAG(vectorStore, embeddingService)

        // 创建文档分割器
        val splitter = RecursiveCharacterTextSplitter(
            chunkSize = 500,
            chunkOverlap = 100
        )

        // 加载文档...

        // 创建 RAG 代理
        val ragAgent = agent {
            name = "FastEmbed RAG Agent"
            instructions = """
                你是一个基于本地嵌入模型的问答助手。

                上下文信息：
                {{context}}

                用户问题：
                {{question}}
            """.trimIndent()
            model = openai
        }

        // 使用 RAG 系统回答问题
        val question = "人工智能的应用领域有哪些？"
        val context = rag.generateContextWithMetadata(question)
        val prompt = ragAgent.instructions
            .replace("{{context}}", context)
            .replace("{{question}}", question)
        val answer = ragAgent.generate(prompt).text
        println(answer)
    } finally {
        // 关闭 FastEmbed 服务，释放资源
        embeddingService.close()
    }
}
```

## 下一步

- 查看 [RAG 系统设计与使用指南](rag-system.md) 了解更多详细信息
- 探索 [RAG 示例](../examples/src/main/kotlin/ai/kastrax/examples/RAGExample.kt) 和 [FastEmbed RAG 示例](../examples/src/main/kotlin/ai/kastrax/examples/FastEmbedRAGExample.kt) 获取更多使用示例
- 了解如何 [自定义 RAG 系统组件](rag-system.md#高级功能) 以满足特定需求
