package ai.kastrax.examples.hello

import kotlinx.coroutines.runBlocking

/**
 * 一个简单的 Agent 示例
 */
fun main() = runBlocking {
    println("Hello Agent 示例")
    println("-------------------")

    // 准备一些问题
    val questions = listOf(
        "什么是人工智能？",
        "如何学习编程？",
        "请解释量子计算的基本原理。"
    )

    // 模拟代理回答
    val responses = listOf(
        """
        人工智能（AI）是计算机科学的一个分支，致力于开发能够模拟人类智能的系统。它包括机器学习、自然语言处理、计算机视觉和知识表示等领域。

        主要特点：
        1. 学习能力：从数据中学习模式和规律
        2. 推理能力：基于学到的知识做出决策
        3. 感知能力：理解和解释环境信息

        应用实例：自动驾驶汽车、智能助手、医疗诊断系统、推荐系统等。
        """.trimIndent(),

        """
        学习编程的有效方法：

        1. 选择合适的编程语言：初学者可以选择Python、JavaScript或Java等语言开始。

        2. 利用在线资源：
           - 免费课程：freeCodeCamp、Codecademy、LeetCode
           - 文档和教程：W3Schools、MDN Web Docs
           - 视频课程：YouTube、Coursera、Udemy

        3. 动手实践：编程是实践性技能，创建小项目并解决实际问题。

        4. 建立学习计划：设定具体目标和时间表，保持持续学习。

        5. 加入编程社区：GitHub、Stack Overflow、编程论坛等。

        关键是坚持实践和解决实际问题，编程能力会随着经验的积累而提高。
        """.trimIndent(),

        """
        量子计算的基本原理：

        量子计算利用量子力学的独特性质来处理信息，与经典计算机使用的二进制位（0和1）不同，量子计算使用量子比特（qubit）。

        主要原理：

        1. 叠加态：与经典比特只能处于0或0不同，qubit可以同时存在于多个状态的叠加中，这种现象称为量子叠加。

        2. 量子纪缉：当多个qubit相互关联时，它们的状态无法独立描述，这种现象称为量子纪缉。

        3. 量子门：量子计算使用量子门来操作qubit，类似于经典计算中的逻辑门。

        4. 测量：当我们测量qubit时，它的叠加态将崩溃为一个经典状态（0或1）。

        量子计算的优势在于可以并行处理大量信息，对于特定问题（如因式分解、量子模拟和某些优化问题）可能比经典计算机快得多。
        """.trimIndent()
    )

    // 模拟代理回答问题
    questions.forEachIndexed { index, question ->
        println("\n问题 ${index + 1}: $question")
        println("生成回答中...")

        // 模拟响应
        val response = responses[index]

        println("\n回答 ${index + 1}:")
        println("----------")
        println(response)
    }

    println("\nHello Agent 示例完成")
}
