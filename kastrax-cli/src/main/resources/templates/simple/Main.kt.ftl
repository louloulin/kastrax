import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

fun main() = runBlocking {
    logger.info { "Starting ${projectName} application" }
    
    val agent = SimpleAgent()
    val response = agent.generate("Tell me about artificial intelligence")
    
    println("Agent response:")
    println(response.text)
    
    logger.info { "Application completed" }
}
