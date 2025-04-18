package ai.kastrax.integrations.openai

import ai.kastrax.core.llm.LlmProvider

/**
 * Factory function to create an OpenAI provider.
 *
 * @param apiKey OpenAI API key
 * @param model OpenAI model ID
 * @param baseUrl Base URL for OpenAI API
 * @param organization Optional OpenAI organization ID
 * @return OpenAI provider instance
 */
fun openAi(
    apiKey: String,
    model: String,
    baseUrl: String = "https://api.openai.com/v1",
    organization: String? = null
): LlmProvider {
    return OpenAiProvider(
        apiKey = apiKey,
        model = model,
        baseUrl = baseUrl,
        organization = organization
    )
}

/**
 * Factory function to create an OpenAI provider with environment variable API key.
 *
 * @param model OpenAI model ID
 * @param baseUrl Base URL for OpenAI API
 * @param organization Optional OpenAI organization ID
 * @return OpenAI provider instance
 * @throws IllegalStateException if OPENAI_API_KEY environment variable is not set
 */
fun openAi(
    model: String,
    baseUrl: String = "https://api.openai.com/v1",
    organization: String? = null
): LlmProvider {
    val apiKey = System.getenv("OPENAI_API_KEY") ?: 
        throw IllegalStateException("OPENAI_API_KEY environment variable not set")
    
    return OpenAiProvider(
        apiKey = apiKey,
        model = model,
        baseUrl = baseUrl,
        organization = organization
    )
}

/**
 * Common OpenAI model IDs.
 */
object OpenAiModels {
    const val GPT_4 = "gpt-4"
    const val GPT_4_TURBO = "gpt-4-turbo"
    const val GPT_4_VISION = "gpt-4-vision-preview"
    const val GPT_4O = "gpt-4o"
    const val GPT_3_5_TURBO = "gpt-3.5-turbo"
}
