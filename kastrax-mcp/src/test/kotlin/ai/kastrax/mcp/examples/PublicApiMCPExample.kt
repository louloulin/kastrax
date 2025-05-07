package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Public API MCP Example
 * This example demonstrates how to use the Kastrax MCP client to connect to various public APIs.
 */
fun main() = runBlocking {
    // 查找可用端口
    val serverPort = ai.kastrax.core.utils.NetworkUtils.findAvailablePort()
    println("Starting Public API MCP Example...")

    // Create MCP server
    val server = mcpServer {
        // Set server name and version
        name("PublicApiMCPServer")
        version("1.0.0")

        // Add joke tool
        tool {
            name = "get_joke"
            description = "Get a random joke"

            // Add parameters
            parameters {
                parameter {
                    name = "category"
                    description = "Joke category (optional)"
                    type = "string"
                    required = false
                }
            }

            // Set execution function
            handler { params ->
                val category = params["category"] as? String
                println("Executing get_joke tool, category: $category")

                // Call the JokeAPI
                val url = if (category != null) {
                    URL("https://v2.jokeapi.dev/joke/$category")
                } else {
                    URL("https://v2.jokeapi.dev/joke/Any")
                }

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        // Parse the JSON response
                        val jsonResponse = Json.parseToJsonElement(response).jsonObject

                        // Extract joke information
                        val type = jsonResponse["type"]?.jsonPrimitive?.content

                        if (type == "single") {
                            val joke = jsonResponse["joke"]?.jsonPrimitive?.content ?: "No joke found"
                            joke
                        } else {
                            val setup = jsonResponse["setup"]?.jsonPrimitive?.content ?: "No setup found"
                            val delivery = jsonResponse["delivery"]?.jsonPrimitive?.content ?: "No delivery found"
                            "$setup\n$delivery"
                        }
                    } else {
                        "Error: Unable to fetch joke. Response code: $responseCode"
                    }
                } catch (e: Exception) {
                    "Error: ${e.message}"
                } finally {
                    connection.disconnect()
                }
            }
        }

        // Add quote tool
        tool {
            name = "get_quote"
            description = "Get a random quote"

            // Set execution function
            handler { _ ->
                println("Executing get_quote tool")

                // Call the QuoteGarden API
                val url = URL("https://quote-garden.onrender.com/api/v3/quotes/random")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        // Parse the JSON response
                        val jsonResponse = Json.parseToJsonElement(response).jsonObject
                        val data = jsonResponse["data"]?.jsonArray?.getOrNull(0)?.jsonObject

                        if (data != null) {
                            val quoteText = data["quoteText"]?.jsonPrimitive?.content ?: "No quote found"
                            val quoteAuthor = data["quoteAuthor"]?.jsonPrimitive?.content ?: "Unknown"

                            "\"$quoteText\" - $quoteAuthor"
                        } else {
                            "No quote found"
                        }
                    } else {
                        "Error: Unable to fetch quote. Response code: $responseCode"
                    }
                } catch (e: Exception) {
                    "Error: ${e.message}"
                } finally {
                    connection.disconnect()
                }
            }
        }

        // Add fact tool
        tool {
            name = "get_fact"
            description = "Get a random fact"

            // Set execution function
            handler { _ ->
                println("Executing get_fact tool")

                // Call the UselessFacts API
                val url = URL("https://uselessfacts.jsph.pl/api/v2/facts/random")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        // Parse the JSON response
                        val jsonResponse = Json.parseToJsonElement(response).jsonObject
                        val text = jsonResponse["text"]?.jsonPrimitive?.content ?: "No fact found"

                        text
                    } else {
                        "Error: Unable to fetch fact. Response code: $responseCode"
                    }
                } catch (e: Exception) {
                    "Error: ${e.message}"
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    // Start server
    server.startSSE(port = serverPort)
    println("MCP server started on port $serverPort")

    // Wait for server to start
    Thread.sleep(1000)

    // Create MCP client
    val client = mcpClient {
        // Set client name and version
        name("PublicApiMCPClient")
        version("1.0.0")

        // Use local server
        server {
            sse {
                url = "http://localhost:$serverPort"
            }
        }
    }

    try {
        // Connect to server
        println("Connecting to MCP server...")
        client.connect()
        println("Connected to MCP server")

        // Get server information
        println("\nServer information:")
        println("Name: ${client.name}")
        println("Version: ${client.version}")

        // Get available tools
        val tools = client.tools()
        println("\nAvailable tools:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }

        // Call joke tool
        println("\nCalling joke tool...")
        val jokeCategories = listOf("Programming", "Misc", "Pun", "Spooky", "Christmas")

        for (category in jokeCategories) {
            val result = client.callTool("get_joke", mapOf("category" to category))
            println("Joke ($category): $result")
        }

        // Call quote tool
        println("\nCalling quote tool...")
        val quoteResult = client.callTool("get_quote", emptyMap())
        println("Quote: $quoteResult")

        // Call fact tool
        println("\nCalling fact tool...")
        val factResult = client.callTool("get_fact", emptyMap())
        println("Fact: $factResult")

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        // Disconnect
        client.disconnect()
        server.stop()
        println("MCP server and client stopped")

        // Add a delay to ensure all resources are released
        kotlinx.coroutines.delay(1000)

        // Force exit program
        kotlin.system.exitProcess(0)
    }
}
