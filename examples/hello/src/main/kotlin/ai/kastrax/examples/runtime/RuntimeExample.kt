package ai.kastrax.examples.runtime

import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import ai.kastrax.runtime.coroutines.KastraxCoroutineInitializer
import ai.kastrax.runtime.coroutines.jvm.JvmCoroutineRuntime

/**
 * 协程运行时示例
 */
fun main() {
    // 1. 初始化协程运行时
    KastraxCoroutineInitializer.initialize(JvmCoroutineRuntime())
    
    // 2. 使用协程运行时
    KastraxCoroutineGlobal.runBlocking {
        println("Starting runtime example...")
        
        // 并行执行多个任务
        val jobs = List(5) { index ->
            KastraxCoroutineGlobal.launch(this) {
                // 切换到 IO 调度器
                KastraxCoroutineGlobal.withIO {
                    println("Task $index running on IO dispatcher")
                    Thread.sleep(100) // 模拟 IO 操作
                }
                
                // 切换到计算调度器
                KastraxCoroutineGlobal.withCompute {
                    println("Task $index running on compute dispatcher")
                    // 模拟计算密集型操作
                    var sum = 0L
                    for (i in 1..1_000_000) {
                        sum += i
                    }
                    println("Task $index computed sum: $sum")
                }
            }
        }
        
        // 等待所有任务完成
        jobs.forEach { it.join() }
        
        println("All tasks completed!")
    }
}
