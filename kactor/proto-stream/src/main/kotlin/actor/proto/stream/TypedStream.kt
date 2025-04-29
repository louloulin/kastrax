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
 * TypedStream 类用于创建一个类型化的消息流
 * @param T 流中消息的类型
 * @property channel 用于接收消息的通道
 * @property pid 流的 PID
 * @property actorSystem Actor 系统
 */
class TypedStream<T : Any>(
    private val channel: Channel<T>,
    private val pid: PID,
    private val actorSystem: ActorSystem
) {
    /**
     * 获取接收消息的通道
     * @return 只读的消息通道
     */
    fun channel(): Channel<T> = channel

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
         * 创建一个新的类型化流
         * @param actorSystem Actor 系统
         * @param T 流中消息的类型
         * @return 类型化流
         */
        inline fun <reified T : Any> create(actorSystem: ActorSystem): TypedStream<T> {
            val channel = Channel<T>()

            val props = Props().withProducer {
                object : Actor {
                    override suspend fun Context.receive(msg: Any) {
                        // 处理消息包装
                        val message = when (msg) {
                            is MessageEnvelope -> msg.message
                            else -> msg
                        }

                        // 检查消息类型
                        if (message is T) {
                            println("TypedStream received message: $message")
                            channel.send(message)
                        } else {
                            println("TypedStream ignoring message of type: ${message?.javaClass}")
                        }
                    }
                }
            }

            val pid = actorSystem.root.spawn(props)

            return TypedStream(channel, pid, actorSystem)
        }
    }
}
