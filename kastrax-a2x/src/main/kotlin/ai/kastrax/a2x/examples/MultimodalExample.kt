package ai.kastrax.a2x.examples

import ai.kastrax.a2x.multimodal.*
import ai.kastrax.a2x.semantic.Context
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 多模态处理示例
 */
fun main() {
    println("=== 多模态处理示例 ===")

    // 创建多模态处理器
    val multimodalProcessor = MultimodalProcessor()

    // 注册处理器
    multimodalProcessor.registerModalityProcessor(TextProcessor())
    multimodalProcessor.registerModalityProcessor(ImageProcessor())
    multimodalProcessor.registerModalityProcessor(AudioProcessor())
    multimodalProcessor.registerModalityProcessor(VideoProcessor())

    // 注册融合策略
    multimodalProcessor.registerFusionStrategy("simple", SimpleFusionStrategy())
    multimodalProcessor.registerFusionStrategy("weighted", WeightedFusionStrategy())

    // 创建上下文
    val context = Context(
        id = "ctx-example",
        name = "示例上下文",
        description = "多模态处理示例上下文",
        type = "example",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        data = buildJsonObject {
            put("location", JsonPrimitive("office"))
            put("time", JsonPrimitive("morning"))
        }
    )

    // 创建多模态输入
    val input = MultimodalInput(
        modalityInputs = mapOf(
            "text" to JsonPrimitive("这是一段描述图片的文本"),
            "image" to JsonPrimitive("https://example.com/image.jpg")
        ),
        context = context
    )

    // 使用简单融合策略处理输入
    println("\n1. 使用简单融合策略处理输入")
    multimodalProcessor.setFusionStrategy("simple")
    val simpleResult = multimodalProcessor.processMultimodalInput(input)

    // 打印结果
    println("处理结果:")
    println("- 模态数量: ${simpleResult.modalityResults.size}")
    println("- 融合结果: ${simpleResult.fusedResult}")

    // 使用加权融合策略处理输入
    println("\n2. 使用加权融合策略处理输入")
    multimodalProcessor.setFusionStrategy("weighted")
    val weightedResult = multimodalProcessor.processMultimodalInput(input)

    // 打印结果
    println("处理结果:")
    println("- 模态数量: ${weightedResult.modalityResults.size}")
    println("- 融合结果: ${weightedResult.fusedResult}")

    // 分析跨模态关系
    println("\n3. 分析跨模态关系")
    val relationships = multimodalProcessor.analyzeCrossModalRelationships(input)

    // 打印关系
    println("跨模态关系:")
    relationships.forEach { relationship ->
        println("- ${relationship.sourceModality} ${relationship.type} ${relationship.targetModality} (强度: ${relationship.strength})")
    }

    // 添加更多模态
    println("\n4. 添加更多模态")
    val multiInput = MultimodalInput(
        modalityInputs = mapOf(
            "text" to JsonPrimitive("这是一段描述图片和音频的文本"),
            "image" to JsonPrimitive("https://example.com/image.jpg"),
            "audio" to JsonPrimitive("https://example.com/audio.mp3"),
            "video" to JsonPrimitive("https://example.com/video.mp4")
        ),
        context = context
    )

    // 处理多模态输入
    val multiResult = multimodalProcessor.processMultimodalInput(multiInput)

    // 打印结果
    println("处理结果:")
    println("- 模态数量: ${multiResult.modalityResults.size}")
    println("- 融合结果: ${multiResult.fusedResult}")

    // 分析多模态关系
    println("\n5. 分析多模态关系")
    val multiRelationships = multimodalProcessor.analyzeCrossModalRelationships(multiInput)

    // 打印关系
    println("跨模态关系:")
    multiRelationships.forEach { relationship ->
        println("- ${relationship.sourceModality} ${relationship.type} ${relationship.targetModality} (强度: ${relationship.strength})")
    }

    println("\n=== 示例结束 ===")
}
