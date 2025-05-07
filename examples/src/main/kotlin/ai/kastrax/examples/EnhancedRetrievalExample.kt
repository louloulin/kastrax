package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.TextFileDocumentLoader
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.reranker.CompositeReranker
import ai.kastrax.rag.reranker.DiversityReranker
import ai.kastrax.rag.reranker.DiversityRerankerConfig
import ai.kastrax.rag.reranker.KeywordMatchReranker
import ai.kastrax.rag.reranker.EnhancedMetadataReranker
import ai.kastrax.rag.reranker.EnhancedMetadataRerankerConfig
import ai.kastrax.rag.reranker.MetadataFieldConfig
import ai.kastrax.rag.reranker.MetadataFieldType
import ai.kastrax.rag.reranker.SortDirection
import ai.kastrax.rag.retrieval.EnhancedHybridRetriever
import ai.kastrax.rag.retrieval.EnhancedHybridRetrieverConfig
import ai.kastrax.rag.retrieval.HybridStrategy
import ai.kastrax.rag.retrieval.TfIdfKeywordExtractor
import ai.kastrax.rag.vectorstore.RagInMemoryVectorStore
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 增强检索示例，展示了如何使用增强高级检索功能。
 */
fun main() = runBlocking {
    println("KastraX 增强检索示例")
    println("===================")

    // 创建向量存储和嵌入服务
    val vectorStore = RagInMemoryVectorStore()
    val embeddingService = RandomEmbeddingService(dimensions = 1536)

    // 创建关键词提取器
    val keywordExtractor = TfIdfKeywordExtractor()

    // 创建多样性重排序器
    val diversityReranker = DiversityReranker(
        embeddingService = embeddingService,
        config = DiversityRerankerConfig(
            diversityWeight = 0.6,
            originalScoreWeight = 0.4
        )
    )

    // 创建关键词匹配重排序器
    val keywordReranker = KeywordMatchReranker(
        keywordWeight = 0.5,
        originalScoreWeight = 0.5
    )

    // 创建元数据重排序器
    val metadataReranker = EnhancedMetadataReranker(
        config = EnhancedMetadataRerankerConfig(
            fields = listOf(
                MetadataFieldConfig(
                    fieldName = "date",
                    fieldType = MetadataFieldType.DATE,
                    weight = 0.8,
                    direction = SortDirection.DESCENDING
                ),
                MetadataFieldConfig(
                    fieldName = "category",
                    fieldType = MetadataFieldType.TEXT,
                    weight = 0.5
                )
            ),
            metadataScoreWeight = 0.6,
            originalScoreWeight = 0.4
        )
    )

    // 创建组合重排序器
    val compositeReranker = CompositeReranker(
        diversityReranker,
        keywordReranker,
        metadataReranker
    )

    // 创建RAG系统
    val rag = RAG(vectorStore, embeddingService, compositeReranker)

    // 创建示例文件目录
    val examplesDir = File("examples_data")
    if (!examplesDir.exists()) {
        examplesDir.mkdirs()
    }

    // 创建示例文件
    createExampleFiles(examplesDir)

    // 加载文档
    val textFiles = examplesDir.listFiles { file -> file.extension == "txt" } ?: emptyArray()
    for (file in textFiles) {
        val loader = TextFileDocumentLoader(file)
        rag.loadDocuments(loader)
    }

    println("\n已加载 ${textFiles.size} 个文档")

    // 创建Agent
    val agent = agent {
        name = "增强检索助手"
        instructions = """
            你是一个增强检索助手，可以回答关于示例数据的问题。
            使用提供的上下文来回答问题。
            如果上下文中没有相关信息，请说明你不知道。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
    }

    // 测试不同的检索策略
    testRetrievalStrategies(rag, agent)
}

/**
 * 测试不同的检索策略。
 */
private suspend fun testRetrievalStrategies(rag: RAG, agent: ai.kastrax.core.agent.Agent) {
    val query = "人工智能的最新发展"

    println("\n查询: $query")

    // 测试加权混合策略
    println("\n1. 加权混合策略:")
    val weightedResults = rag.search(
        query = query,
        limit = 3,
        options = RagProcessOptions(
            useEnhancedHybridSearch = true,
            enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                hybridStrategy = HybridStrategy.WEIGHTED,
                vectorWeight = 0.7,
                keywordWeight = 0.3
            )
        )
    )

    printResults(weightedResults)

    // 测试交错混合策略
    println("\n2. 交错混合策略:")
    val interleaveResults = rag.search(
        query = query,
        limit = 3,
        options = RagProcessOptions(
            useEnhancedHybridSearch = true,
            enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                hybridStrategy = HybridStrategy.INTERLEAVE,
                vectorWeight = 0.7,
                keywordWeight = 0.3
            )
        )
    )

    printResults(interleaveResults)

    // 测试分层混合策略
    println("\n3. 分层混合策略:")
    val hierarchicalResults = rag.search(
        query = query,
        limit = 3,
        options = RagProcessOptions(
            useEnhancedHybridSearch = true,
            enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                hybridStrategy = HybridStrategy.HIERARCHICAL,
                vectorWeight = 0.7,
                keywordWeight = 0.3
            )
        )
    )

    printResults(hierarchicalResults)

    // 测试多样性重排序
    println("\n4. 多样性重排序:")
    val diversityResults = rag.search(
        query = query,
        limit = 3,
        options = RagProcessOptions(
            useReranking = true
        )
    )

    printResults(diversityResults)

    // 使用最佳策略生成回答
    println("\n使用最佳策略生成回答:")
    val context = rag.generateContext(
        query = query,
        limit = 3,
        options = RagProcessOptions(
            useEnhancedHybridSearch = true,
            enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                hybridStrategy = HybridStrategy.WEIGHTED,
                vectorWeight = 0.7,
                keywordWeight = 0.3
            ),
            useReranking = true
        )
    )

    val response = agent.generate(
        """
        基于以下上下文回答问题:

        $context

        问题: $query
        """.trimIndent()
    )

    println("回答: ${response.text}")
}

/**
 * 打印搜索结果。
 */
private fun printResults(results: List<ai.kastrax.rag.vectorstore.SearchResult>) {
    results.forEachIndexed { index, result ->
        println("${index + 1}. ${result.document.metadata["title"]} (分数: ${String.format("%.2f", result.score)})")
        println("   类别: ${result.document.metadata["category"]}, 日期: ${result.document.metadata["date"]}")
        println("   ${result.document.content.take(100)}...")
    }
}

/**
 * 创建示例文件。
 */
private fun createExampleFiles(dir: File) {
    // 创建人工智能文章
    createTextFile(
        dir,
        "ai_intro.txt",
        """
        人工智能简介

        人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于研究和开发能够模拟人类智能的系统。这些系统能够学习、推理、感知、规划和解决问题。

        人工智能的主要目标是创造能够像人类一样思考和行动的机器。这包括理解自然语言、识别图像和声音、做出决策以及适应新环境的能力。

        人工智能的应用非常广泛，包括但不限于：自动驾驶汽车、语音助手、推荐系统、医疗诊断、金融分析和游戏。
        """.trimIndent(),
        mapOf(
            "title" to "人工智能简介",
            "category" to "AI",
            "date" to "2022-01-15"
        )
    )

    // 创建机器学习文章
    createTextFile(
        dir,
        "machine_learning.txt",
        """
        机器学习基础

        机器学习是人工智能的一个子领域，它专注于开发能够从数据中学习的算法和模型。机器学习系统通过分析大量数据来识别模式，然后使用这些模式进行预测或决策。

        机器学习的主要类型包括：

        1. 监督学习：算法从带标签的训练数据中学习，然后对新数据进行预测。
        2. 无监督学习：算法从没有标签的数据中发现隐藏的模式或结构。
        3. 强化学习：算法通过与环境交互并接收反馈来学习最佳行动。

        常见的机器学习算法包括线性回归、决策树、随机森林、支持向量机和神经网络。
        """.trimIndent(),
        mapOf(
            "title" to "机器学习基础",
            "category" to "ML",
            "date" to "2022-03-20"
        )
    )

    // 创建深度学习文章
    createTextFile(
        dir,
        "deep_learning.txt",
        """
        深度学习入门

        深度学习是机器学习的一个子领域，它使用多层神经网络来模拟人脑的工作方式。深度学习模型能够自动从大量数据中提取特征，而不需要人工特征工程。

        深度学习的核心是深度神经网络，它由多个隐藏层组成。每一层都从前一层提取更高级别的特征。这种层次结构使深度学习模型能够学习复杂的表示。

        深度学习在图像识别、自然语言处理、语音识别和游戏等领域取得了突破性的成果。例如，卷积神经网络（CNN）在图像识别中表现出色，而循环神经网络（RNN）和变换器（Transformer）在自然语言处理中表现出色。
        """.trimIndent(),
        mapOf(
            "title" to "深度学习入门",
            "category" to "DL",
            "date" to "2022-05-10"
        )
    )

    // 创建自然语言处理文章
    createTextFile(
        dir,
        "nlp.txt",
        """
        自然语言处理技术

        自然语言处理（Natural Language Processing，简称NLP）是人工智能的一个分支，它专注于使计算机能够理解、解释和生成人类语言。NLP技术使计算机能够与人类进行自然的交流。

        NLP的主要任务包括：

        1. 文本分类：将文本分类到预定义的类别中。
        2. 情感分析：确定文本表达的情感是积极的、消极的还是中性的。
        3. 命名实体识别：识别文本中的实体，如人名、地名和组织名。
        4. 机器翻译：将文本从一种语言翻译成另一种语言。
        5. 问答系统：回答用自然语言提出的问题。
        6. 文本生成：生成自然、连贯的文本。

        近年来，基于Transformer架构的模型，如BERT、GPT和T5，在NLP任务中取得了显著的进展。
        """.trimIndent(),
        mapOf(
            "title" to "自然语言处理技术",
            "category" to "NLP",
            "date" to "2022-07-05"
        )
    )

    // 创建计算机视觉文章
    createTextFile(
        dir,
        "computer_vision.txt",
        """
        计算机视觉概述

        计算机视觉是人工智能的一个分支，它使计算机能够从数字图像或视频中获取高级理解。计算机视觉系统能够识别物体、人脸、场景和活动。

        计算机视觉的主要任务包括：

        1. 图像分类：将图像分类到预定义的类别中。
        2. 物体检测：识别图像中的物体并定位它们。
        3. 图像分割：将图像分割成多个区域或物体。
        4. 人脸识别：识别和验证人脸。
        5. 姿态估计：确定人或物体的姿态。
        6. 视频分析：分析视频中的活动和事件。

        深度学习，特别是卷积神经网络（CNN），已经彻底改变了计算机视觉领域，使系统能够达到甚至超过人类在某些任务上的表现。
        """.trimIndent(),
        mapOf(
            "title" to "计算机视觉概述",
            "category" to "CV",
            "date" to "2022-09-15"
        )
    )

    // 创建强化学习文章
    createTextFile(
        dir,
        "reinforcement_learning.txt",
        """
        强化学习原理

        强化学习是机器学习的一个分支，它专注于如何使智能体在环境中采取行动以最大化累积奖励。强化学习不同于监督学习和无监督学习，它不需要标记数据，而是通过试错和反馈来学习。

        强化学习的核心概念包括：

        1. 智能体：做出决策的实体。
        2. 环境：智能体交互的世界。
        3. 状态：环境的当前情况。
        4. 行动：智能体可以采取的行动。
        5. 奖励：环境对智能体行动的反馈。
        6. 策略：智能体的决策规则。

        强化学习已经在游戏（如AlphaGo）、机器人控制、资源管理和推荐系统等领域取得了显著的成果。
        """.trimIndent(),
        mapOf(
            "title" to "强化学习原理",
            "category" to "RL",
            "date" to "2022-11-20"
        )
    )

    // 创建生成式AI文章
    createTextFile(
        dir,
        "generative_ai.txt",
        """
        生成式人工智能的最新进展

        生成式人工智能是人工智能的一个快速发展的领域，它专注于创建能够生成新内容的模型。这些模型可以生成文本、图像、音乐、视频和其他类型的媒体。

        生成式AI的主要类型包括：

        1. 生成对抗网络（GAN）：由生成器和判别器组成，通过对抗训练生成逼真的内容。
        2. 变分自编码器（VAE）：通过学习数据的潜在表示来生成新内容。
        3. 自回归模型：如GPT系列，通过预测序列中的下一个元素来生成内容。
        4. 扩散模型：如DALL-E和Stable Diffusion，通过逐步去噪过程生成图像。

        最近的进展包括多模态模型（如GPT-4和Claude），它们能够理解和生成多种类型的内容，以及生成式AI在创意领域的广泛应用。
        """.trimIndent(),
        mapOf(
            "title" to "生成式人工智能的最新进展",
            "category" to "GenAI",
            "date" to "2023-01-10"
        )
    )

    // 创建大型语言模型文章
    createTextFile(
        dir,
        "llm.txt",
        """
        大型语言模型的发展与应用

        大型语言模型（Large Language Models，简称LLM）是一种基于Transformer架构的神经网络，它们通过预训练在大量文本数据上学习语言的统计模式。这些模型能够理解、生成和翻译人类语言。

        大型语言模型的发展历程：

        1. 2018年：BERT的发布标志着预训练语言模型的兴起。
        2. 2020年：GPT-3展示了大规模语言模型的能力。
        3. 2022年：ChatGPT的发布使大型语言模型进入大众视野。
        4. 2023年：GPT-4、Claude和Llama 2等模型进一步推动了技术进步。

        大型语言模型的应用非常广泛，包括内容创作、代码生成、客户服务、教育辅助、研究助手和创意写作等。然而，这些模型也面临着幻觉、偏见和安全性等挑战。
        """.trimIndent(),
        mapOf(
            "title" to "大型语言模型的发展与应用",
            "category" to "LLM",
            "date" to "2023-03-25"
        )
    )

    // 创建AI伦理文章
    createTextFile(
        dir,
        "ai_ethics.txt",
        """
        人工智能伦理与治理

        随着人工智能技术的快速发展和广泛应用，AI伦理和治理变得越来越重要。AI伦理关注的是如何确保AI系统的开发和使用符合道德标准和社会价值观。

        AI伦理的主要关注点包括：

        1. 公平性和偏见：确保AI系统不会放大或延续社会中的不公平和偏见。
        2. 透明度和可解释性：使AI系统的决策过程透明且可解释。
        3. 隐私和数据保护：保护用户数据和隐私权。
        4. 安全性和可靠性：确保AI系统安全可靠，不会造成伤害。
        5. 问责制：明确AI系统开发和使用中的责任归属。
        6. 人类自主权：保持人类对AI系统的控制和决策权。

        全球各国和组织正在制定AI伦理准则和监管框架，以指导AI的负责任发展和使用。
        """.trimIndent(),
        mapOf(
            "title" to "人工智能伦理与治理",
            "category" to "Ethics",
            "date" to "2023-05-15"
        )
    )

    // 创建AI在医疗中的应用文章
    createTextFile(
        dir,
        "ai_healthcare.txt",
        """
        人工智能在医疗健康中的应用

        人工智能正在彻底改变医疗健康行业，提高诊断准确性、个性化治疗方案和医疗资源的分配效率。

        AI在医疗中的主要应用包括：

        1. 医学影像分析：AI可以分析X光片、CT扫描和MRI图像，帮助医生检测疾病。
        2. 疾病诊断：AI系统可以分析患者数据和症状，辅助医生进行诊断。
        3. 药物发现：AI加速了新药的研发过程，预测药物的效果和副作用。
        4. 个性化医疗：AI可以根据患者的基因组和健康历史定制治疗方案。
        5. 远程医疗：AI支持的远程医疗服务使医疗更加便捷和普及。
        6. 健康监测：AI可以分析可穿戴设备收集的数据，监测健康状况并预测潜在问题。

        尽管AI在医疗中有巨大潜力，但也面临数据隐私、监管和临床整合等挑战。
        """.trimIndent(),
        mapOf(
            "title" to "人工智能在医疗健康中的应用",
            "category" to "Healthcare",
            "date" to "2023-07-20"
        )
    )

    // 创建AI未来趋势文章
    createTextFile(
        dir,
        "ai_future.txt",
        """
        人工智能的未来趋势

        人工智能技术正在以前所未有的速度发展，塑造着我们的未来。以下是一些值得关注的AI未来趋势：

        1. 多模态AI：能够处理和生成多种类型数据（文本、图像、音频、视频）的AI系统将变得更加普遍。

        2. AI民主化：随着工具和平台的发展，AI技术将变得更加易于访问和使用，使更多人能够创建和部署AI解决方案。

        3. 自主系统：具有更高自主性的AI系统将能够在更少人类干预的情况下完成复杂任务。

        4. 边缘AI：AI计算将更多地在边缘设备上进行，减少对云计算的依赖，提高响应速度和隐私保护。

        5. AI与人类协作：人机协作将成为主流，AI系统将作为人类的助手和增强工具，而不是替代品。

        6. 可解释AI：随着对AI透明度和可解释性的需求增加，可解释AI技术将得到进一步发展。

        7. 神经符号AI：结合神经网络和符号推理的混合系统将能够结合两种方法的优势。

        8. 可持续AI：更加注重能源效率和环境影响的AI系统将得到发展。

        这些趋势将共同推动AI技术的进步，并扩大其在各个领域的应用。
        """.trimIndent(),
        mapOf(
            "title" to "人工智能的未来趋势",
            "category" to "Trends",
            "date" to "2023-09-30"
        )
    )
}

/**
 * 创建文本文件。
 */
private fun createTextFile(dir: File, fileName: String, content: String, metadata: Map<String, String>) {
    val file = File(dir, fileName)
    file.writeText(content)
}
