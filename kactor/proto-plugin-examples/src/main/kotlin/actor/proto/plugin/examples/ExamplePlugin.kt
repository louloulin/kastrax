package actor.proto.plugin.examples

import actor.proto.ActorSystem
import actor.proto.plugin.ProtoPlugin
import org.pf4j.Extension
import org.pf4j.PluginWrapper
import org.slf4j.LoggerFactory

/**
 * 示例插件
 * 包含多个扩展点实现
 */
class ExamplePlugin(wrapper: PluginWrapper) : ProtoPlugin(wrapper) {
    private val logger = LoggerFactory.getLogger(ExamplePlugin::class.java)
    
    override fun start() {
        logger.info("ExamplePlugin started")
    }
    
    override fun stop() {
        logger.info("ExamplePlugin stopped")
    }
    
    override fun init(system: ActorSystem) {
        logger.info("ExamplePlugin initialized with ActorSystem: {}", system.name)
    }
}
