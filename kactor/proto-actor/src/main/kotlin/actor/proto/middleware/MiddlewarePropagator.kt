package actor.proto.middleware

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.Props
import actor.proto.ReceiveMiddleware
import actor.proto.SenderMiddleware
import actor.proto.SpawnerContext

/**
 * 中间件传播器，用于将中间件传播到子 Actor
 */
class MiddlewarePropagator {
    private val spawnMiddleware = mutableListOf<SpawnMiddleware>()
    private val senderMiddleware = mutableListOf<SenderMiddleware>()
    private val receiverMiddleware = mutableListOf<ReceiveMiddleware>()
    private val contextDecorator = mutableListOf<ContextDecorator>()

    /**
     * 将自身作为 Spawn 中间件传播
     * @return 中间件传播器
     */
    fun withItselfForwarded(): MiddlewarePropagator {
        return withSpawnMiddleware({ next -> this.spawnMiddleware()(next) })
    }

    /**
     * 添加 Spawn 中间件
     * @param middleware Spawn 中间件
     * @return 中间件传播器
     */
    fun withSpawnMiddleware(vararg middleware: SpawnMiddleware): MiddlewarePropagator {
        spawnMiddleware.addAll(middleware)
        return this
    }

    /**
     * 添加发送中间件
     * @param middleware 发送中间件
     * @return 中间件传播器
     */
    fun withSenderMiddleware(vararg middleware: SenderMiddleware): MiddlewarePropagator {
        senderMiddleware.addAll(middleware)
        return this
    }

    /**
     * 添加接收中间件
     * @param middleware 接收中间件
     * @return 中间件传播器
     */
    fun withReceiverMiddleware(vararg middleware: ReceiveMiddleware): MiddlewarePropagator {
        receiverMiddleware.addAll(middleware)
        return this
    }

    /**
     * 添加上下文装饰器
     * @param decorator 上下文装饰器
     * @return 中间件传播器
     */
    fun withContextDecorator(vararg decorator: ContextDecorator): MiddlewarePropagator {
        contextDecorator.addAll(decorator)
        return this
    }

    /**
     * 创建 Spawn 中间件
     * @return Spawn 中间件
     */
    fun spawnMiddleware(): SpawnMiddleware {
        return { next ->
            { system, id, props, parentContext ->
                var newProps = props

                if (spawnMiddleware.isNotEmpty()) {
                    newProps = newProps.copy(spawnMiddleware = spawnMiddleware.toList())
                }

                if (senderMiddleware.isNotEmpty()) {
                    newProps = newProps.copy(senderMiddleware = senderMiddleware.toList())
                }

                if (receiverMiddleware.isNotEmpty()) {
                    newProps = newProps.copy(receiveMiddleware = receiverMiddleware.toList())
                }

                if (contextDecorator.isNotEmpty()) {
                    newProps = newProps.copy(contextDecorator = contextDecorator.toList())
                }

                next(system, id, newProps, parentContext)
            }
        }
    }

    companion object {
        /**
         * 创建新的中间件传播器
         * @return 中间件传播器
         */
        fun create(): MiddlewarePropagator = MiddlewarePropagator()
    }
}
