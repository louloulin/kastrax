package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Weather API MCP Example
 * This example demonstrates how to use the Kastrax MCP client to connect to a public weather API.
 */
fun main() = runBlocking {
    // 查找可用端口
    val serverPort = ai.kastrax.core.utils.NetworkUtils.findAvailablePort()
    println("Starting Weather API MCP Example...")

    // Create MCP server
    val server = mcpServer {
        // Set server name and version
        name("WeatherApiMCPServer")
        version("1.0.0")

        // Add weather tool
        tool {
            name = "get_weather"
            description = "Get current weather for a city"

            // Add parameters
            parameters {
                parameter {
                    name = "city"
                    description = "City name"
                    type = "string"
                    required = true
                }
            }

            // Set execution function
            handler { params ->
                val city = params["city"] as? String ?: "London"
                println("Executing get_weather tool, city: $city")

                // Call the OpenWeatherMap API
                // Note: In a real application, you would use your own API key
                // This is using a sample API key for demonstration purposes only
                val apiKey = "b6907d289e10d714a6e88b30761fae22" // Sample API key from OpenWeatherMap
                val url = URL("https://samples.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey")

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }

                        // Parse the JSON response
                        val jsonResponse = Json.parseToJsonElement(response).jsonObject

                        // Extract weather information
                        val main = jsonResponse["main"]?.jsonObject
                        val weatherArray = jsonResponse["weather"]?.jsonArray
                        val weatherObject = weatherArray?.getOrNull(0)?.jsonObject
                        val temperature = main?.get("temp")?.jsonPrimitive?.content ?: "N/A"
                        val description = weatherObject?.get("description")?.jsonPrimitive?.content ?: "N/A"

                        "Weather in $city: Temperature: $temperature°C, Description: $description"
                    } else {
                        "Error: Unable to fetch weather data. Response code: $responseCode"
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
        name("WeatherApiMCPClient")
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

        // Call weather tool
        println("\nCalling weather tool...")
        val cities = listOf("London", "New York", "Tokyo", "Sydney", "Paris")

        for (city in cities) {
            val result = client.callTool("get_weather", mapOf("city" to city))
            println("Result for $city: $result")
        }

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
