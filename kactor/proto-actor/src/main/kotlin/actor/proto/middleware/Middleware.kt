package actor.proto.middleware

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.SpawnerContext

/**
 * 定义 Spawn 中间件函数类型
 * 用于在 Actor 创建过程中添加额外的行为
 */
typealias SpawnMiddleware = (SpawnFunc) -> SpawnFunc

/**
 * 定义 Spawn 函数类型
 * 用于创建 Actor 并返回其 PID
 */
typealias SpawnFunc = (ActorSystem, String, Props, SpawnerContext) -> PID

/**
 * 定义 Context 装饰器函数类型
 * 用于扩展 Context 的功能
 */
typealias ContextDecorator = (ContextDecoratorFunc) -> ContextDecoratorFunc

/**
 * 定义 Context 装饰器函数类型
 * 用于装饰 Context
 */
typealias ContextDecoratorFunc = (Context) -> Context

/**
 * 创建 Receiver 中间件链
 * @param receiverMiddleware 接收中间件列表
 * @param lastReceiver 最终接收函数
 * @return 中间件链
 */
fun makeReceiverMiddlewareChain(receiverMiddleware: List<actor.proto.ReceiveMiddleware>, lastReceiver: actor.proto.Receive): actor.proto.Receive {
    if (receiverMiddleware.isEmpty()) {
        return lastReceiver
    }

    var h = receiverMiddleware.last()(lastReceiver)
    for (i in receiverMiddleware.size - 2 downTo 0) {
        h = receiverMiddleware[i](h)
    }

    return h
}

/**
 * 创建 Sender 中间件链
 * @param senderMiddleware 发送中间件列表
 * @param lastSender 最终发送函数
 * @return 中间件链
 */
fun makeSenderMiddlewareChain(senderMiddleware: List<actor.proto.SenderMiddleware>, lastSender: actor.proto.Send): actor.proto.Send {
    if (senderMiddleware.isEmpty()) {
        return lastSender
    }

    var h = senderMiddleware.last()(lastSender)
    for (i in senderMiddleware.size - 2 downTo 0) {
        h = senderMiddleware[i](h)
    }

    return h
}

/**
 * 创建 Context 装饰器链
 * @param decorator 装饰器列表
 * @param lastDecorator 最终装饰器函数
 * @return 装饰器链
 */
fun makeContextDecoratorChain(decorator: List<ContextDecorator>, lastDecorator: ContextDecoratorFunc): ContextDecoratorFunc {
    if (decorator.isEmpty()) {
        return lastDecorator
    }

    var h = decorator.last()(lastDecorator)
    for (i in decorator.size - 2 downTo 0) {
        h = decorator[i](h)
    }

    return h
}

/**
 * 创建 Spawn 中间件链
 * @param spawnMiddleware Spawn 中间件列表
 * @param lastSpawn 最终 Spawn 函数
 * @return 中间件链
 */
fun makeSpawnMiddlewareChain(spawnMiddleware: List<SpawnMiddleware>, lastSpawn: SpawnFunc): SpawnFunc {
    if (spawnMiddleware.isEmpty()) {
        return lastSpawn
    }

    var h = spawnMiddleware.last()(lastSpawn)
    for (i in spawnMiddleware.size - 2 downTo 0) {
        h = spawnMiddleware[i](h)
    }

    return h
}
