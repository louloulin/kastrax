package ai.kastrax.codebase.indexing.distributed

import actor.proto.ActorSystem
import ai.kastrax.codebase.indexing.IndexProcessor

/**
 * 分布式索引系统工厂
 */
object DistributedIndexSystemFactory {
    /**
     * 创建分布式索引系统
     *
     * @param indexProcessor 索引处理器
     * @param actorSystem Actor 系统
     * @param config 系统配置
     * @return 分布式索引系统
     */
    fun createDistributedIndexSystem(
        indexProcessor: ai.kastrax.codebase.indexing.IndexProcessor,
        actorSystem: ActorSystem,
        config: DistributedIndexSystemConfig = DistributedIndexSystemConfig()
    ): DistributedIndexSystem {
        return DistributedIndexSystem(
            actorSystem = actorSystem,
            indexProcessor = indexProcessor,
            config = config
        )
    }
}
