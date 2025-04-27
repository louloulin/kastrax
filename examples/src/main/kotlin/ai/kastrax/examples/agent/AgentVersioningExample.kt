package ai.kastrax.examples.agent

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.version.AgentVersionStatus
import ai.kastrax.core.agent.version.InMemoryAgentVersionManager
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking

/**
 * Agent版本控制和回滚示例
 */
fun main() = runBlocking {
    println("KastraX Agent版本控制和回滚示例")
    println("----------------------------")

    // 创建版本管理器
    val versionManager = InMemoryAgentVersionManager()

    // 创建Agent
    val myAgent = agent {
        name = "VersionedAssistant"
        instructions = "You are a helpful assistant."
        model = openAi(
            model = "gpt-4o",
            // API key from environment variable OPENAI_API_KEY
        )
        versionManager(versionManager)
    }

    // 获取初始版本
    val initialVersion = myAgent.getActiveVersion()
    println("\n初始版本:")
    println("ID: ${initialVersion?.id}")
    println("版本号: ${initialVersion?.versionNumber}")
    println("状态: ${initialVersion?.status}")
    println("指令: ${initialVersion?.instructions}")

    // 使用初始版本生成响应
    val response1 = myAgent.generate("What can you do?")
    println("\n使用初始版本的响应:")
    println(response1.text)

    // 创建新版本
    val newVersion = myAgent.createVersion(
        instructions = "You are a helpful and friendly assistant specialized in explaining complex topics in simple terms.",
        name = "Friendly Explainer",
        description = "更友好的解释者版本",
        activateImmediately = true
    )
    println("\n创建新版本:")
    println("ID: ${newVersion?.id}")
    println("版本号: ${newVersion?.versionNumber}")
    println("状态: ${newVersion?.status}")
    println("指令: ${newVersion?.instructions}")

    // 使用新版本生成响应
    val response2 = myAgent.generate("What can you do?")
    println("\n使用新版本的响应:")
    println(response2.text)

    // 获取所有版本
    val allVersions = myAgent.getVersions()
    println("\n所有版本:")
    allVersions?.forEach { version ->
        println("${version.versionNumber}. ${version.name} (${version.status})")
    }

    // 回滚到初始版本
    val rolledBackVersion = myAgent.rollbackToVersion(initialVersion!!.id)
    println("\n回滚到初始版本:")
    println("ID: ${rolledBackVersion?.id}")
    println("版本号: ${rolledBackVersion?.versionNumber}")
    println("状态: ${rolledBackVersion?.status}")

    // 使用回滚后的版本生成响应
    val response3 = myAgent.generate("What can you do?")
    println("\n使用回滚版本的响应:")
    println(response3.text)

    // 创建另一个版本但不激活
    val draftVersion = myAgent.createVersion(
        instructions = "You are a technical assistant specialized in programming and software development.",
        name = "Technical Assistant",
        description = "技术助手版本",
        activateImmediately = false
    )
    println("\n创建草稿版本:")
    println("ID: ${draftVersion?.id}")
    println("版本号: ${draftVersion?.versionNumber}")
    println("状态: ${draftVersion?.status}")

    // 获取当前激活版本
    val currentActiveVersion = myAgent.getActiveVersion()
    println("\n当前激活版本:")
    println("ID: ${currentActiveVersion?.id}")
    println("版本号: ${currentActiveVersion?.versionNumber}")
    println("指令: ${currentActiveVersion?.instructions}")

    // 激活草稿版本
    val activatedVersion = myAgent.activateVersion(draftVersion!!.id)
    println("\n激活草稿版本:")
    println("ID: ${activatedVersion?.id}")
    println("版本号: ${activatedVersion?.versionNumber}")
    println("状态: ${activatedVersion?.status}")

    // 使用新激活的版本生成响应
    val response4 = myAgent.generate("What can you do?")
    println("\n使用新激活版本的响应:")
    println(response4.text)

    // 获取最终的所有版本
    val finalVersions = myAgent.getVersions()
    println("\n最终所有版本:")
    finalVersions?.forEach { version ->
        println("${version.versionNumber}. ${version.name} (${version.status})")
    }
}
