package actor.proto.plugin

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Props
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderMiddleware

/**
 * 插件接口
 * 所有插件必须实现此接口
 */
interface PluginInterface {
    /**
     * 获取插件ID
     * @return 插件ID
     */
    fun id(): String
    
    /**
     * 获取插件版本
     * @return 插件版本
     */
    fun version(): String
    
    /**
     * 初始化插件
     * @param system Actor系统
     */
    fun init(system: ActorSystem)
    
    /**
     * 关闭插件
     */
    fun shutdown()
}

/**
 * 接收中间件插件接口
 */
interface ReceiveMiddlewarePluginInterface : PluginInterface {
    /**
     * 获取接收中间件
     * @return 接收中间件
     */
    fun receiveMiddleware(): ReceiveMiddleware
}

/**
 * 发送中间件插件接口
 */
interface SenderMiddlewarePluginInterface : PluginInterface {
    /**
     * 获取发送中间件
     * @return 发送中间件
     */
    fun senderMiddleware(): SenderMiddleware
}

/**
 * Actor装饰器插件接口
 */
interface ActorDecoratorPluginInterface : PluginInterface {
    /**
     * 装饰Actor
     * @param actor 原始Actor
     * @return 装饰后的Actor
     */
    fun decorateActor(actor: Actor): Actor
}

/**
 * Props装饰器插件接口
 */
interface PropsDecoratorPluginInterface : PluginInterface {
    /**
     * 装饰Props
     * @param props 原始Props
     * @return 装饰后的Props
     */
    fun decorateProps(props: Props): Props
}
