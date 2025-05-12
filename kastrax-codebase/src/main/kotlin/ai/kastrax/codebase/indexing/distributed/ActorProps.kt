package ai.kastrax.codebase.indexing.distributed

import actor.proto.PID
import actor.proto.Props
import ai.kastrax.codebase.indexing.IndexProcessor

/**
 * Actor Props 工厂方法
 */
object ActorProps {
    /**
     * 创建索引协调器 Actor 的 Props
     *
     * @param config 配置
     * @return Props
     */
    fun IndexCoordinatorActor.Companion.props(config: IndexCoordinatorConfig): Props {
        return actor.proto.fromProducer {
            IndexCoordinatorActor(config)
        }
    }

    /**
     * 创建索引工作器 Actor 的 Props
     *
     * @param workerId 工作器ID
     * @param coordinatorPid 协调器PID
     * @param indexProcessor 索引处理器
     * @param config 配置
     * @return Props
     */
    fun IndexWorkerActor.Companion.props(
        workerId: String,
        coordinatorPid: PID,
        indexProcessor: IndexProcessor,
        config: IndexWorkerConfig
    ): Props {
        return actor.proto.fromProducer {
            IndexWorkerActor(workerId, coordinatorPid, indexProcessor, config)
        }
    }

    /**
     * 创建索引分片管理器 Actor 的 Props
     *
     * @param config 配置
     * @return Props
     */
    fun IndexShardManager.Companion.props(config: IndexShardManagerConfig): Props {
        return actor.proto.fromProducer {
            IndexShardManager(config)
        }
    }
}
