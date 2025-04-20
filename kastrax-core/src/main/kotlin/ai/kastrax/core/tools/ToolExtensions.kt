package ai.kastrax.core.tools

import ai.kastrax.zod.Schema
import ai.kastrax.zod.parseJson
import ai.kastrax.zod.toJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json

/**
 * 将传统的 Tool 转换为 ZodTool。
 */
inline fun <reified I, reified O> Tool.toZodTool(
    inputSchema: Schema<I, I>,
    outputSchema: Schema<O, O>? = null
): ZodTool<I, O> {
    return object : ZodTool<I, O> {
        override val id: String = this@toZodTool.id
        override val name: String = this@toZodTool.name
        override val description: String = this@toZodTool.description
        override val inputSchema: Schema<I, I> = inputSchema
        override val outputSchema: Schema<O, O>? = outputSchema

        override suspend fun execute(input: I): O {
            val jsonInput = inputSchema.toJson(input)
            val jsonOutput = this@toZodTool.execute(jsonInput)
            return outputSchema?.parseJson(jsonOutput) ?: jsonOutput as O
        }

        override suspend fun executeWithContext(
            input: I,
            threadId: String?,
            resourceId: String?
        ): O {
            val jsonInput = inputSchema.toJson(input)
            val jsonOutput = this@toZodTool.executeWithContext(jsonInput, threadId, resourceId)
            return outputSchema?.parseJson(jsonOutput) ?: jsonOutput as O
        }
    }
}
