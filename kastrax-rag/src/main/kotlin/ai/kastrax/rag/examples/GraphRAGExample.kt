package ai.kastrax.rag.examples

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.embedding.OpenAIEmbeddingService
import ai.kastrax.rag.graph.GraphRAG
import ai.kastrax.rag.graph.GraphRAGConfig
import ai.kastrax.rag.graph.GraphRAGQueryOptions
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import java.util.Scanner

/**
 * GraphRAG 示例
 */
fun main() = runBlocking {
    println("正在初始化 GraphRAG 系统...")

    // 创建向量存储
    val vectorStore = InMemoryVectorStore()

    // 创建嵌入服务（需要 OpenAI API 密钥）
    val openaiApiKey = System.getenv("OPENAI_API_KEY") ?: ""
    if (openaiApiKey.isEmpty()) {
        println("请设置 OPENAI_API_KEY 环境变量")
        return@runBlocking
    }

    val embeddingService = OpenAIEmbeddingService(openaiApiKey)

    // 创建 GraphRAG 系统
    val graphRAG = GraphRAG(
        GraphRAGConfig(
            dimension = 1536,
            threshold = 0.7,
            bidirectional = true
        )
    )

    // 示例文档
    val text = """
        # 河谷高地：社区发展研究

        ## 历史背景
        河谷高地的中心区于1932年在汤普森钢铁厂周围建立。意大利移民马可·罗西在附近开了一家小杂货店，主要为工厂工人服务。最初的工厂选址是由于其靠近水路和铁路运输路线的战略位置，为未来的交通走廊奠定了基础。

        ## 交通发展
        南北铁路线项目于1973年开始建设，承诺改善区域连通性。初步调查确定了沿拟议路线的几个具有历史意义的区域，包括市场区的一些最古老的部分。这一时期的交通管理局记录指出，在实施现代铁路基础设施的同时维护现有社区通道的技术挑战。

        ## 经济转变
        1970年代中期标志着河谷高地的企业迁移显著时期。主要基础设施项目的完成导致几家长期存在的企业搬迁，包括历史悠久的罗西市场主要位置。到2000年，运营成本上升迫使汤普森钢铁厂关闭其主要设施。中村投资集团于2002年购买了废弃的工厂综合体，最初计划建造豪华公寓。

        ## 文化变迁
        社区紧张局势在1970年代交通扩张期间达到顶峰，组织抗议破坏既定的社区模式。由陈玛丽亚于2005年创立的东区艺术集体，开始通过在废弃店面的临时装置记录这些历史变化。他们的"工业记忆"项目展示了前钢铁工人家庭的照片，其中几张显示罗西家族的杂货店在各种社会变革时期作为社区聚集场所。

        ## 环境倡议
        2010年启动的河流恢复项目，在旧汤普森钢铁厂附近发现了严重的工业污染。历史记录显示，包括早期铁路建设和后续扩建在内的各种基础设施项目，创造了影响自然水流模式的人工障碍。项目首席科学家詹姆斯·汤普森三世博士建议进行广泛的土壤修复，并呼吁对交通基础设施的长期环境影响进行全面研究。

        ## 城市规划
        市议会2015年的重新分区倡议将前工业区指定为混合用途文化区。重新分区承认各种交通走廊的历史意义，包括铁路线和社区通道。中村集团的原始开发计划被修改，纳入了几条曾经连接钢铁厂与各罗西市场位置的历史步行道。

        ## 社区项目
        由原钢铁公司继承人萨拉·汤普森-陈建立的汤普森基金会，专注于青少年环境科学教育。他们的旗舰项目在一座翻新的罗西市场大楼中运营，教导学生关于城市生态和可持续发展。基金会的课程特别研究不同阶段的交通发展如何塑造当地环境条件。

        ## 本地商业
        夜市倡议由大卫·中村与当地艺术家合作于2020年启动，将前钢铁厂停车场变成每周社区活动。几位供应商是汤普森基金会小企业计划的毕业生。市场的位置专门选择在历史步行路线和现代交通连接都能到达的地方。一个受欢迎的摊位由安东尼奥·罗西经营，提供他祖父原始商店的食谱。

        ## 基础设施发展
        最近的城市规划文件显示，地铁交通管理局正在考虑扩建铁路系统的东线。拟议的路线将需要拆除几个艺术集体空间，但将改善通往夜市区域的通道。历史保护倡导者指出，这一扩建将影响1975年前时期仅存的一些原始市场区结构。

        ## 未来前景
        中村集团最近宣布计划在剩余的汤普森钢铁厂建筑中资助"遗产创新中心"。该项目旨在将艺术集体成员的工作空间与河流恢复项目的环境监测站结合起来。设计融入了原始罗西市场建筑的元素，承认其历史意义。中心的位置选择是为了通过现有铁路网络和传统社区通道最大化可达性。
    """.trimIndent()

    // 分割文档
    val document = Document(content = text)
    val splitter = RecursiveCharacterTextSplitter(
        chunkSize = 512,
        chunkOverlap = 50,
        separators = listOf("\n\n", "\n", "。", "，", " ")
    )
    val chunks = splitter.split(document).map { it.content }

    // 创建文档
    val documents = chunks.map { chunk ->
        Document(
            content = chunk,
            metadata = mapOf("source" to "河谷高地研究")
        )
    }

    println("正在生成嵌入向量...")

    // 生成嵌入向量
    val embeddings = documents.map { document ->
        embeddingService.embed(document.content)
    }

    // 创建图
    println("正在构建知识图谱...")
    graphRAG.createGraph(documents, embeddings)

    println("\nGraphRAG 系统已准备就绪！")
    println("节点数量: ${graphRAG.getNodes().size}")
    println("边数量: ${graphRAG.getEdges().size}")
    println("\n你可以输入查询，或输入 'exit' 退出")

    val scanner = Scanner(System.`in`)

    while (true) {
        print("\n查询: ")
        val query = scanner.nextLine().trim()

        if (query.equals("exit", ignoreCase = true)) {
            break
        }

        println("\n正在检索相关信息...")

        // 生成查询嵌入向量
        val queryEmbedding = embeddingService.embed(query)

        // 查询图
        val results = graphRAG.query(
            queryEmbedding,
            GraphRAGQueryOptions(topK = 3)
        )

        if (results.isEmpty()) {
            println("未找到相关信息。")
            continue
        }

        println("\n检索结果:")
        results.forEachIndexed { index, result ->
            println("${index + 1}. 相似度: ${String.format("%.2f", result.score)}")
            println("   内容: ${result.content.take(150)}...")
            println()
        }
    }

    println("\nGraphRAG 系统已退出")
}
