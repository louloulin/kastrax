package actor.proto.plugin.examples

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.plugin.passivation.Passivate
import actor.proto.plugin.passivation.PassivationPlugin
import actor.proto.plugin.persistence.InMemoryPersistenceProvider
import actor.proto.plugin.persistence.PersistencePlugin
import actor.proto.plugin.persistence.PersistentActor
import actor.proto.plugin.polyglot.GraalPlugin
import actor.proto.plugin.polyglot.loadJavaScriptPlugin
import actor.proto.plugin.polyglot.loadScriptPlugins
import org.pf4j.DefaultPluginManager
import org.pf4j.PluginWrapper
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 插件系统示例
 */
fun main() {
    println("=== Plugin System Example ===")

    // 创建Actor系统
    val system = ActorSystem.create()

    // 创建插件
    val manager = DefaultPluginManager()
    val passivationDescriptor = org.pf4j.DefaultPluginDescriptor("passivation", "Passivation Plugin", "1.0.0", "Test", null, null, null)
    val persistenceDescriptor = org.pf4j.DefaultPluginDescriptor("persistence", "Persistence Plugin", "1.0.0", "Test", null, null, null)
    val graalDescriptor = org.pf4j.DefaultPluginDescriptor("graal", "Graal Plugin", "1.0.0", "Test", null, null, null)

    val passivationPlugin = PassivationPlugin(PluginWrapper(manager, passivationDescriptor, null, null))
    val persistencePlugin = PersistencePlugin(PluginWrapper(manager, persistenceDescriptor, null, null))
    val graalPlugin = GraalPlugin(PluginWrapper(manager, graalDescriptor, null, null))

    // 注册插件
    system.registerPlugin(passivationPlugin)
    system.registerPlugin(persistencePlugin)
    system.registerPlugin(graalPlugin)

    // 配置持久化插件
    val provider = InMemoryPersistenceProvider.create()
    persistencePlugin.setProvider(provider)

    // 加载JavaScript插件
    val scriptPath = Paths.get("plugins/test-plugin.js").toAbsolutePath().toString()
    system.loadJavaScriptPlugin(scriptPath)

    println("Plugins registered:")
    println("- Passivation Plugin (ID: ${passivationPlugin.id()}, Version: ${passivationPlugin.version()})")
    println("- Persistence Plugin (ID: ${persistencePlugin.id()}, Version: ${persistencePlugin.version()})")
    println("- GraalVM Plugin (ID: ${graalPlugin.id()}, Version: ${graalPlugin.version()})")

    // 创建一个使用被动化插件的Actor
    val passivationLatch = CountDownLatch(1)
    val passivationProps = fromProducer {
        object : Actor {
            override suspend fun Context.receive(msg: Any) {
                when (msg) {
                    is String -> {
                        println("PassivationActor received: $msg")
                    }
                    is Passivate -> {
                        println("PassivationActor received Passivate message")
                        passivationLatch.countDown()
                    }
                }
            }
        }
    }

    val passivationPid = system.actorOf(passivationProps)

    // 发送消息
    system.send(passivationPid, "Hello from PassivationActor")

    // 等待被动化
    println("Waiting for passivation...")
    passivationLatch.await(2, TimeUnit.SECONDS)

    // 创建一个使用持久化插件的Actor
    val persistenceLatch = CountDownLatch(1)
    val persistenceProps = fromProducer {
        object : PersistentActor {
            override suspend fun Context.receive(msg: Any) {
                when (msg) {
                    is String -> {
                        println("PersistentActor received: $msg")
                        persistenceLatch.countDown()
                    }
                }
            }

            override suspend fun onRecover(context: Context, event: Any) {
                println("PersistentActor recovering event: $event")
            }

            override suspend fun onRecoverSnapshot(context: Context, snapshot: Any) {
                println("PersistentActor recovering snapshot: $snapshot")
            }

            override suspend fun onPersist(context: Context, event: Any) {
                println("PersistentActor persisted event: $event")
            }
        }
    }

    val persistencePid = system.actorOf(persistenceProps)

    // 发送消息
    system.send(persistencePid, "Hello from PersistentActor")

    // 等待持久化
    println("Waiting for persistence...")
    persistenceLatch.await(2, TimeUnit.SECONDS)

    // 关闭Actor系统
    system.shutdown()

    println("Example completed")
}
