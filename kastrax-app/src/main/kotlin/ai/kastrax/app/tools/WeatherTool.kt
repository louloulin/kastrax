package ai.kastrax.app.tools

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import ai.kastrax.app.config.loadConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import com.google.gson.Gson

private val logger = KotlinLogging.logger {}

/**
 * 天气工具。
 * 用于获取天气信息。
 */
val weatherTool = tool {
    // 设置 ID 和名称
    id = "weather"
    name = "天气查询"

    // 设置描述
    description = "一个天气工具，可以获取指定位置的天气信息"

    // 定义输入模式
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("location") {
                put("type", "string")
                put("description", "位置（城市名或坐标）")
            }
            putJsonObject("units") {
                put("type", "string")
                put("description", "单位（metric 或 imperial）")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("location"))
        }
    }

    // 执行天气查询
    execute = { input ->
        try {
            val inputStr = input.toString()
            val location = if (inputStr.contains("location")) {
                inputStr.substringAfter("location").substringAfter(":").substringAfter("\"").substringBefore("\"")
            } else {
                "未知位置"
            }

            val units = if (inputStr.contains("units")) {
                inputStr.substringAfter("units").substringAfter(":").substringAfter("\"").substringBefore("\"")
            } else {
                "metric"
            }

            logger.info { "查询天气: $location, 单位: $units" }

            // 在实际应用中，这里应该调用真实的天气 API
            // 这里使用模拟数据作为示例
            val weatherData = getWeatherData(location, units)

            buildJsonObject {
                put("location", weatherData.location)
                put("temperature", weatherData.temperature)
                put("conditions", weatherData.conditions)
                put("humidity", weatherData.humidity)
                put("windSpeed", weatherData.windSpeed)
                put("units", units)
            }
        } catch (e: Exception) {
            logger.error(e) { "获取天气信息时发生错误" }
            buildJsonObject {
                put("error", "天气查询错误: ${e.message}")
            }
        }
    }
}

/**
 * 天气数据类。
 */
data class WeatherData(
    val location: String,
    val temperature: Double,
    val conditions: String,
    val humidity: Int,
    val windSpeed: Double
)

/**
 * 获取天气数据。
 *
 * 注意：这是一个模拟实现，实际应用中应该调用真实的天气 API。
 */
private fun getWeatherData(location: String, units: String): WeatherData {
    // 在实际应用中，这里应该调用真实的天气 API
    // 例如 OpenWeatherMap、WeatherAPI 等

    // 模拟数据
    return when (location.lowercase()) {
        "beijing", "北京" -> WeatherData(
            location = "Beijing",
            temperature = 25.0,
            conditions = "Sunny",
            humidity = 45,
            windSpeed = 5.0
        )
        "shanghai", "上海" -> WeatherData(
            location = "Shanghai",
            temperature = 28.0,
            conditions = "Cloudy",
            humidity = 60,
            windSpeed = 8.0
        )
        "new york", "纽约" -> WeatherData(
            location = "New York",
            temperature = 22.0,
            conditions = "Rainy",
            humidity = 75,
            windSpeed = 12.0
        )
        else -> WeatherData(
            location = location,
            temperature = 20.0 + (0..10).random(),
            conditions = listOf("Sunny", "Cloudy", "Rainy", "Windy").random(),
            humidity = (30..80).random(),
            windSpeed = (2..15).random().toDouble()
        )
    }
}
