package ai.kastrax.examples.streaming

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

/**
 * 基础流式代理示例
 * 展示如何使用KastraX进行流式文本生成
 */
fun main() = runBlocking {
    println("KastraX 基础流式代理示例")
    println("========================")

    // 创建代理
    val agent = agent {
        name = "Streaming Writer"
        instructions = """
            你是一个专业的写作助手，擅长创作各种类型的文本内容。
            请用中文回答问题，并提供详细、有用的信息。
            在回答时要有逻辑性和条理性。
        """.trimIndent()
        
        // 注意：实际使用时需要配置正确的模型
        // model = openAi("gpt-4")
    }

    // 示例1：简单流式生成
    println("\n=== 示例1：简单流式生成 ===")
    println("用户: 请写一篇关于春天的短文")
    
    val response1 = agent.stream("请写一篇关于春天的短文")
    
    print("助手: ")
    response1.textStream?.collect { chunk ->
        print(chunk)
        delay(50) // 模拟打字机效果
    }
    println("\n")

    // 示例2：带字符统计的流式生成
    println("=== 示例2：带字符统计的流式生成 ===")
    println("用户: 解释一下什么是人工智能")
    
    val response2 = agent.stream("解释一下什么是人工智能")
    
    print("助手: ")
    var totalChars = 0
    response2.textStream?.collect { chunk ->
        print(chunk)
        totalChars += chunk.length
        
        // 每100个字符显示一次统计
        if (totalChars % 100 == 0) {
            print(" [${totalChars}字符] ")
        }
        
        delay(40)
    }
    println("\n总字符数: $totalChars\n")

    // 示例3：分段显示的流式生成
    println("=== 示例3：分段显示的流式生成 ===")
    println("用户: 请列出学习编程的5个步骤")
    
    val response3 = agent.stream("请列出学习编程的5个步骤")
    
    print("助手: ")
    var currentLine = ""
    response3.textStream?.collect { chunk ->
        currentLine += chunk
        print(chunk)
        
        // 检测到换行时显示分段标记
        if (chunk.contains("\n")) {
            print(" [段落结束] ")
            currentLine = ""
        }
        
        delay(30)
    }
    println("\n")

    // 示例4：实时词汇统计
    println("=== 示例4：实时词汇统计 ===")
    println("用户: 介绍一下机器学习的主要算法")
    
    val response4 = agent.stream("介绍一下机器学习的主要算法")
    
    print("助手: ")
    var wordCount = 0
    var currentWord = ""
    
    response4.textStream?.collect { chunk ->
        print(chunk)
        
        // 简单的词汇计数
        chunk.forEach { char ->
            if (char.isWhitespace() || char in "，。！？；：") {
                if (currentWord.isNotEmpty()) {
                    wordCount++
                    currentWord = ""
                }
            } else {
                currentWord += char
            }
        }
        
        // 每20个词显示一次统计
        if (wordCount > 0 && wordCount % 20 == 0) {
            print(" [${wordCount}词] ")
        }
        
        delay(35)
    }
    
    // 处理最后一个词
    if (currentWord.isNotEmpty()) {
        wordCount++
    }
    
    println("\n总词数: $wordCount\n")

    // 示例5：带进度指示的流式生成
    println("=== 示例5：带进度指示的流式生成 ===")
    println("用户: 写一个关于未来科技的故事")
    
    val response5 = agent.stream("写一个关于未来科技的故事")
    
    print("助手: ")
    var chunkCount = 0
    val progressMarkers = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
    
    response5.textStream?.collect { chunk ->
        print(chunk)
        chunkCount++
        
        // 每50个chunk显示进度
        if (chunkCount % 50 == 0) {
            val progress = progressMarkers[(chunkCount / 50 - 1) % progressMarkers.size]
            print(" $progress ")
        }
        
        delay(25)
    }
    println("\n生成完成！总块数: $chunkCount\n")

    // 示例6：错误处理和重试
    println("=== 示例6：错误处理和重试 ===")
    
    try {
        println("用户: 请用代码示例解释递归算法")
        val response6 = agent.stream("请用代码示例解释递归算法")
        
        print("助手: ")
        response6.textStream?.collect { chunk ->
            print(chunk)
            delay(30)
        }
        println("\n")
        
    } catch (e: Exception) {
        println("流式生成出错: ${e.message}")
        println("尝试使用普通生成模式...")
        
        val fallbackResponse = agent.generate("请简单解释递归算法")
        println("备用回答: ${fallbackResponse.text}\n")
    }

    // 示例7：多种速度的流式生成
    println("=== 示例7：多种速度的流式生成 ===")
    
    val speeds = listOf(
        "慢速" to 100L,
        "中速" to 50L,
        "快速" to 20L
    )
    
    speeds.forEach { (speedName, delay) ->
        println("\n${speedName}生成示例:")
        println("用户: 简单介绍一下量子计算")
        
        val response = agent.stream("简单介绍一下量子计算")
        
        print("助手($speedName): ")
        response.textStream?.collect { chunk ->
            print(chunk)
            delay(delay)
        }
        println("\n")
    }

    println("基础流式代理示例完成！")
}