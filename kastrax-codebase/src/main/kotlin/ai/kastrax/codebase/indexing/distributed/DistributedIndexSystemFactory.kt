package ai.kastrax.codebase.indexing.distributed

import ai.kastrax.codebase.actor.ActorSystem
// TODO: 暂时注释掉，等待依赖问题解决
// import ai.kastrax.codebase.store.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import java.io.File

/**
 * 分布式索引系统工厂
 */
object DistributedIndexSystemFactory {
    /**
     * 创建分布式索引系统
     *
     * @param documentStore 文档存储
     * @param embeddingService 嵌入服务
     * @param rootPath 根路径
     * @param actorSystem Actor 系统
     * @param coordinatorConfig 协调者配置
     * @return 分布式索引系统
     */
    fun createDistributedIndexSystem(
        documentStore: Any,
        embeddingService: Any,
        rootPath: Any,
        actorSystem: Any,
        coordinatorConfig: IndexCoordinatorConfig
    ): DistributedIndexSystem {
        return DistributedIndexSystem(
            documentStore = documentStore,
            embeddingService = embeddingService,
            rootPath = rootPath as File,
            actorSystem = actorSystem,
            coordinatorConfig = coordinatorConfig
        )
    }
}
