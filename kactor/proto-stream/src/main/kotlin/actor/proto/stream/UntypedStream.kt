package actor.proto.stream

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.MessageEnvelope
import actor.proto.PID
import actor.proto.Props
import actor.proto.withProducer
import kotlinx.coroutines.channels.Channel

/**
 * UntypedStream 类用于创建一个非类型化的消息流
 * @property channel 用于接收消息的通道
 * @property pid 流的 PID
 * @property actorSystem Actor 系统
 */
class UntypedStream(
    private val channel: Channel<Any>,
    private val pid: PID,
    private val actorSystem: ActorSystem
) {
    /**
     * 获取接收消息的通道
     * @return 只读的消息通道
     */
    fun channel(): Channel<Any> = channel

    /**
     * 获取流的 PID
     * @return 流的 PID
     */
    fun pid(): PID = pid

    /**
     * 关闭流
     */
    fun close() {
        actorSystem.root.stop(pid)
        channel.close()
    }

    companion object {
        /**
         * 创建一个新的非类型化流
         * @param actorSystem Actor 系统
         * @return 非类型化流
         */
        fun create(actorSystem: ActorSystem): UntypedStream {
            val channel = Channel<Any>()

            val props = Props().withProducer {
                object : Actor {
                    override suspend fun Context.receive(msg: Any) {
                        // 处理消息包装
                        val message = when (msg) {
                            is MessageEnvelope -> msg.message
                            else -> msg
                        }

                        // 将所有非系统消息发送到通道
                        println("UntypedStream received message: $message")
                        channel.send(message)
                    }
                }
            }

            val pid = actorSystem.root.spawn(props)

            return UntypedStream(channel, pid, actorSystem)
        }
    }
}
