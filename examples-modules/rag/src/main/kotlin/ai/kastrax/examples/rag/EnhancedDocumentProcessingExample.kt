package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.filter.CompositeDocumentFilter
import ai.kastrax.rag.document.filter.ContentKeywordFilter
import ai.kastrax.rag.document.filter.ContentLengthFilter
import ai.kastrax.rag.document.filter.MetadataFilter
import ai.kastrax.rag.document.transform.CompositeDocumentTransformer
import ai.kastrax.rag.document.transform.DocumentCleaner
import ai.kastrax.rag.document.transform.DocumentNormalizer
import ai.kastrax.rag.document.transform.MetadataTransformer
import ai.kastrax.rag.document.transform.TextNormalizeTransformer
import ai.kastrax.rag.document.transform.TextReplaceTransformer
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.vectorstore.EnhancedRagVectorStore
import ai.kastrax.rag.vectorstore.EnhancedVectorStore
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 增强文档处理示例，展示了如何使用增强的文档处理和索引功能。
 */
fun main() = runBlocking {
    println("KastraX 增强文档处理示例")
    println("======================")

    // 创建嵌入服务
    val embeddingService = RandomEmbeddingService(dimensions = 1536)

    // 创建增强RAG向量存储
    val ragVectorStore = EnhancedRagVectorStore()

    // 创建Agent
    val agent = agent {
        name = "文档处理助手"
        instructions = """
            你是一个文档处理助手，可以回答关于示例数据的问题。
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

    // 创建示例文档
    val documents = createSampleDocuments()
    println("创建了 ${documents.size} 个示例文档")

    // 演示文档清洗
    demonstrateDocumentCleaning(documents)

    // 演示文档标准化
    demonstrateDocumentNormalization(documents)

    // 演示文档转换
    demonstrateDocumentTransformation(documents)

    // 演示文档过滤
    demonstrateDocumentFiltering(documents)

    println("\n示例完成")
}

/**
 * 创建示例文档。
 */
private fun createSampleDocuments(): List<Document> {
    return listOf(
        Document(
            content = """
                <h1>人工智能简介</h1>
                <p>人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于研究和开发能够模拟人类智能的系统。
                这些系统能够学习、推理、感知、规划和解决问题。</p>
                <p>人工智能的主要目标是创造能够像人类一样思考和行动的机器。这包括理解自然语言、识别图像和声音、
                做出决策以及适应新环境的能力。</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "人工智能简介",
                "category" to "AI",
                "language" to "zh",
                "date" to "2023-01-15",
                "author" to "张三",
                "source" to "AI教程"
            )
        ),
        Document(
            content = """
                <h1>Machine Learning Basics</h1>
                <p>Machine learning is a subset of artificial intelligence that focuses on developing algorithms
                and models that can learn from data. These systems can identify patterns in large datasets
                and make predictions or decisions without being explicitly programmed.</p>
                <p>The main types of machine learning include supervised learning, unsupervised learning,
                and reinforcement learning. Common algorithms include linear regression, decision trees,
                random forests, support vector machines, and neural networks.</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "Machine Learning Basics",
                "category" to "AI",
                "language" to "en",
                "date" to "2023-02-20",
                "author" to "John Smith",
                "source" to "AI Tutorial"
            )
        ),
        Document(
            content = """
                <h1>深度学习入门</h1>
                <p>深度学习是机器学习的一个子领域，它使用多层神经网络来模拟人脑的工作方式。深度学习模型能够自动从大量数据中提取特征，
                而不需要人工特征工程。</p>
                <p>深度学习的核心是深度神经网络，它由多个隐藏层组成。每一层都从前一层提取更高级别的特征。
                这种层次结构使深度学习模型能够学习复杂的表示。</p>
                <p>深度学习在图像识别、自然语言处理、语音识别和游戏等领域取得了突破性的成果。
                例如，卷积神经网络（CNN）在图像识别中表现出色，而循环神经网络（RNN）和变换器（Transformer）在自然语言处理中表现出色。</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "深度学习入门",
                "category" to "AI",
                "language" to "zh",
                "date" to "2023-03-10",
                "author" to "李四",
                "source" to "AI教程"
            )
        ),
        Document(
            content = """
                <h1>Natural Language Processing</h1>
                <p>Natural Language Processing (NLP) is a field of artificial intelligence that focuses on the interaction
                between computers and human language. It enables computers to understand, interpret, and generate
                human language in a valuable way.</p>
                <p>NLP combines computational linguistics, machine learning, and deep learning to process and analyze
                large amounts of natural language data. Key tasks in NLP include text classification, sentiment analysis,
                named entity recognition, machine translation, question answering, and text generation.</p>
                <p>Recent advances in NLP have been driven by transformer-based models like BERT, GPT, and T5,
                which have achieved state-of-the-art results on various language tasks.</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "Natural Language Processing",
                "category" to "AI",
                "language" to "en",
                "date" to "2023-04-05",
                "author" to "Emily Johnson",
                "source" to "AI Tutorial"
            )
        ),
        Document(
            content = """
                <h1>计算机视觉概述</h1>
                <p>计算机视觉是人工智能的一个分支，它使计算机能够从数字图像或视频中获取高级理解。
                计算机视觉系统能够识别物体、人脸、场景和活动。</p>
                <p>计算机视觉的主要任务包括图像分类、物体检测、图像分割、人脸识别、姿态估计和视频分析。
                深度学习，特别是卷积神经网络（CNN），已经彻底改变了计算机视觉领域，使系统能够达到甚至超过人类在某些任务上的表现。</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "计算机视觉概述",
                "category" to "AI",
                "language" to "zh",
                "date" to "2023-05-20",
                "author" to "王五",
                "source" to "AI教程"
            )
        ),
        Document(
            content = """
                <h1>Reinforcement Learning</h1>
                <p>Reinforcement learning is a type of machine learning where an agent learns to make decisions
                by taking actions in an environment to maximize some notion of cumulative reward. Unlike supervised
                and unsupervised learning, reinforcement learning focuses on finding a balance between exploration
                of unknown territory and exploitation of current knowledge.</p>
                <p>Key concepts in reinforcement learning include the agent, environment, state, action, reward,
                and policy. The agent's goal is to learn a policy that maps states to actions in a way that
                maximizes expected cumulative reward.</p>
                <p>Reinforcement learning has achieved remarkable results in games (like AlphaGo), robotics,
                resource management, and recommendation systems.</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "Reinforcement Learning",
                "category" to "AI",
                "language" to "en",
                "date" to "2023-06-15",
                "author" to "David Brown",
                "source" to "AI Tutorial"
            )
        ),
        Document(
            content = """
                <h1>数据科学入门</h1>
                <p>数据科学是一个跨学科领域，它结合了统计学、计算机科学和领域专业知识，从数据中提取有价值的见解和知识。
                数据科学家使用各种技术和工具来分析和解释复杂的数据。</p>
                <p>数据科学的主要步骤包括数据收集、数据清洗、数据探索、特征工程、模型构建、模型评估和结果解释。
                常用的工具和语言包括Python、R、SQL、Pandas、NumPy、Scikit-learn和TensorFlow等。</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "数据科学入门",
                "category" to "Data Science",
                "language" to "zh",
                "date" to "2023-07-10",
                "author" to "赵六",
                "source" to "数据科学教程"
            )
        ),
        Document(
            content = """
                <h1>Big Data Analytics</h1>
                <p>Big data analytics is the process of examining large and varied data sets to uncover hidden patterns,
                unknown correlations, market trends, customer preferences, and other useful business information.
                These insights can help organizations make more informed business decisions.</p>
                <p>Big data is characterized by the three Vs: volume (large amounts of data), velocity (high speed of data),
                and variety (different types of data). Some also add veracity (uncertainty of data) and value
                (usefulness of data) to this list.</p>
                <p>Common big data technologies include Hadoop, Spark, NoSQL databases, and cloud-based data warehouses.
                These technologies enable the processing and analysis of data that would be too large or complex
                for traditional data processing applications.</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "Big Data Analytics",
                "category" to "Data Science",
                "language" to "en",
                "date" to "2023-08-05",
                "author" to "Sarah Wilson",
                "source" to "Data Science Tutorial"
            )
        ),
        Document(
            content = """
                <h1>云计算基础</h1>
                <p>云计算是一种按需提供计算资源（如计算能力、存储和数据库）的模式，这些资源通常通过互联网提供，
                并按使用量付费。云计算使组织能够避免或最小化前期IT基础设施成本。</p>
                <p>云计算的主要服务模式包括：</p>
                <ul>
                    <li>基础设施即服务（IaaS）：提供虚拟化的计算资源</li>
                    <li>平台即服务（PaaS）：提供开发和部署应用程序的平台</li>
                    <li>软件即服务（SaaS）：通过互联网提供应用程序</li>
                </ul>
                <p>主要的云服务提供商包括亚马逊Web服务（AWS）、微软Azure、谷歌云平台（GCP）和阿里云。</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "云计算基础",
                "category" to "Cloud Computing",
                "language" to "zh",
                "date" to "2023-09-20",
                "author" to "孙七",
                "source" to "云计算教程"
            )
        ),
        Document(
            content = """
                <h1>DevOps Principles</h1>
                <p>DevOps is a set of practices that combines software development (Dev) and IT operations (Ops)
                with the goal of shortening the systems development life cycle and providing continuous delivery
                of high-quality software.</p>
                <p>Key DevOps principles include:</p>
                <ul>
                    <li>Continuous Integration (CI): Frequently merging code changes into a central repository</li>
                    <li>Continuous Delivery (CD): Automating the delivery of applications to selected infrastructure environments</li>
                    <li>Infrastructure as Code (IaC): Managing infrastructure through code instead of manual processes</li>
                    <li>Monitoring and Logging: Collecting and analyzing data and logs to improve applications and infrastructure</li>
                    <li>Communication and Collaboration: Breaking down silos between development and operations teams</li>
                </ul>
                <p>DevOps tools include Git, Jenkins, Docker, Kubernetes, Ansible, Terraform, and Prometheus.</p>
            """.trimIndent(),
            metadata = mapOf(
                "title" to "DevOps Principles",
                "category" to "DevOps",
                "language" to "en",
                "date" to "2023-10-15",
                "author" to "Michael Davis",
                "source" to "DevOps Tutorial"
            )
        )
    )
}

/**
 * 演示文档清洗。
 */
private fun demonstrateDocumentCleaning(documents: List<Document>) {
    println("\n文档清洗示例")
    println("------------")

    val cleaner = DocumentCleaner()

    // 基本清洗选项
    val basicOptions = DocumentCleaner.basicCleaningOptions()

    // 高级清洗选项
    val advancedOptions = DocumentCleaner.CleaningOptions(
        removeHtmlTags = true,
        removeExtraWhitespace = true,
        normalizeWhitespace = true,
        normalizePunctuation = true,
        removeEmptyLines = true,
        convertToLowercase = true
    )

    // 选择一个示例文档
    val document = documents[0]
    println("原始文档内容（前100个字符）：${document.content.take(100)}...")

    // 基本清洗
    val basicCleanedDocument = cleaner.clean(document, basicOptions)
    println("\n基本清洗后（前100个字符）：${basicCleanedDocument.content.take(100)}...")

    // 高级清洗
    val advancedCleanedDocument = cleaner.clean(document, advancedOptions)
    println("\n高级清洗后（前100个字符）：${advancedCleanedDocument.content.take(100)}...")
}

/**
 * 演示文档标准化。
 */
private fun demonstrateDocumentNormalization(documents: List<Document>) {
    println("\n文档标准化示例")
    println("--------------")

    val normalizer = DocumentNormalizer()

    // 基本标准化选项
    val basicOptions = DocumentNormalizer.basicNormalizationOptions()

    // 高级标准化选项
    val advancedOptions = DocumentNormalizer.NormalizationOptions(
        unicodeNormalization = true,
        caseNormalization = true,
        caseForm = DocumentNormalizer.CaseForm.LOWERCASE,
        whitespaceNormalization = true,
        punctuationNormalization = true,
        dateNormalization = true,
        dateFormat = "yyyy-MM-dd",
        paragraphNormalization = true,
        removeControlCharacters = true,
        metadataKeysToLowercase = true
    )

    // 选择一个示例文档
    val document = documents[1]
    println("原始文档内容（前100个字符）：${document.content.take(100)}...")
    println("原始元数据：${document.metadata}")

    // 基本标准化
    val basicNormalizedDocument = normalizer.normalize(document, basicOptions)
    println("\n基本标准化后（前100个字符）：${basicNormalizedDocument.content.take(100)}...")
    println("标准化后元数据：${basicNormalizedDocument.metadata}")

    // 高级标准化
    val advancedNormalizedDocument = normalizer.normalize(document, advancedOptions)
    println("\n高级标准化后（前100个字符）：${advancedNormalizedDocument.content.take(100)}...")
    println("标准化后元数据：${advancedNormalizedDocument.metadata}")
}

/**
 * 演示文档转换。
 */
private suspend fun demonstrateDocumentTransformation(documents: List<Document>) {
    println("\n文档转换示例")
    println("------------")

    // 创建各种转换器
    val replaceTransformer = TextReplaceTransformer("<h1>", "# ")
    val normalizeTransformer = TextNormalizeTransformer(
        normalizeWhitespace = true,
        normalizePunctuation = true,
        normalizeCase = false
    )
    val metadataTransformer = MetadataTransformer(
        addMetadata = mapOf("processed" to true, "processed_date" to LocalDate.now().format(DateTimeFormatter.ISO_DATE))
    )

    // 创建组合转换器
    val compositeTransformer = CompositeDocumentTransformer(
        replaceTransformer,
        normalizeTransformer,
        metadataTransformer
    )

    // 选择一个示例文档
    val document = documents[2]
    println("原始文档内容（前100个字符）：${document.content.take(100)}...")
    println("原始元数据：${document.metadata}")

    // 应用转换
    val transformedDocument = compositeTransformer.transform(document)
    println("\n转换后（前100个字符）：${transformedDocument.content.take(100)}...")
    println("转换后元数据：${transformedDocument.metadata}")
}

/**
 * 演示文档过滤。
 */
private fun demonstrateDocumentFiltering(documents: List<Document>) {
    println("\n文档过滤示例")
    println("------------")

    // 创建各种过滤器
    val lengthFilter = ContentLengthFilter(minLength = 500)
    val keywordFilter = ContentKeywordFilter("neural network", "deep learning", matchAll = false)
    val languageFilter = MetadataFilter("language", "zh")
    val dateFilter = MetadataFilter(
        "date",
        "2023-05-01",
        MetadataFilter.MatchMode.GREATER_THAN
    )

    // 创建组合过滤器
    val compositeFilter = CompositeDocumentFilter(
        lengthFilter,
        languageFilter,
        dateFilter,
        mode = CompositeDocumentFilter.FilterMode.ALL
    )

    // 应用过滤器
    val filteredDocuments = compositeFilter.filter(documents)

    println("原始文档数量：${documents.size}")
    println("过滤后文档数量：${filteredDocuments.size}")
    println("\n过滤后的文档标题：")
    filteredDocuments.forEach { document ->
        println("- ${document.metadata["title"]} (${document.metadata["date"]})")
    }
}


