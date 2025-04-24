package ai.kastrax.app.tools

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import ai.kastrax.app.config.loadConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
val weatherTool = tool("weather") {
    description = "一个天气工具，可以获取指定位置的天气信息"
    
    // 定义输入参数
    parameters {
        parameter("location", "string", "位置（城市名或坐标）", true)
        parameter("units", "string", "单位（metric 或 imperial）", false)
    }
    
    // 执行天气查询
    execute { input ->
        try {
            val location = input.get("location").asString()
            val units = input.getOrDefault("units", "metric").asString()
            
            logger.info { "查询天气: $location, 单位: $units" }
            
            // 在实际应用中，这里应该调用真实的天气 API
            // 这里使用模拟数据作为示例
            val weatherData = getWeatherData(location, units)
            
            mapOf(
                "location" to weatherData.location,
                "temperature" to weatherData.temperature,
                "conditions" to weatherData.conditions,
                "humidity" to weatherData.humidity,
                "windSpeed" to weatherData.windSpeed,
                "units" to units
            )
        } catch (e: Exception) {
            logger.error(e) { "获取天气信息时发生错误" }
            mapOf("error" to "天气查询错误: ${e.message}")
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
    return when (location.toLowerCase()) {
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
