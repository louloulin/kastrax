package ai.kastrax.examples.mcp

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 天气查询 MCP 应用案例
 * 
 * 这个示例展示了如何创建一个提供天气查询功能的 MCP 服务器，
 * 以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。
 */
fun main() = runBlocking {
    println("启动天气查询 MCP 应用案例...")
    
    // 启动服务器
    val serverJob = launch {
        val server = createWeatherServer()
        server.startSSE(port = 8081)
        println("天气查询 MCP 服务器已启动在端口 8081")
        
        // 保持服务器运行
        try {
            while (true) {
                delay(1000)
            }
        } finally {
            server.stop()
            println("天气查询 MCP 服务器已停止")
        }
    }
    
    // 等待服务器启动
    delay(2000)
    
    // 创建并使用客户端
    val client = createWeatherClient()
    
    try {
        // 连接到服务器
        println("连接到天气查询 MCP 服务器...")
        client.connect()
        println("已连接到天气查询 MCP 服务器")
        
        // 获取服务器能力
        val hasTools = client.supportsCapability("tools")
        println("服务器支持工具: $hasTools")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 查询不同城市的天气
        val cities = listOf("北京", "上海", "广州", "深圳", "杭州")
        
        for (city in cities) {
            println("\n查询${city}的天气...")
            val weatherResult = client.callTool("get_weather", mapOf("city" to city))
            println("${city}天气: $weatherResult")
        }
        
        // 查询天气预报
        println("\n查询北京的天气预报...")
        val forecastResult = client.callTool("get_forecast", mapOf("city" to "北京", "days" to 3))
        println("北京天气预报: $forecastResult")
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与天气查询 MCP 服务器的连接")
        
        // 停止服务器
        serverJob.cancel()
    }
}

/**
 * 创建天气查询 MCP 服务器
 */
private fun createWeatherServer() = mcpServer {
    name("WeatherMCPServer")
    version("1.0.0")
    
    // 添加天气查询工具
    tool {
        name = "get_weather"
        description = "获取指定城市的当前天气"
        
        // 添加参数
        parameters {
            parameter {
                name = "city"
                description = "城市名称"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val city = params["city"] as? String ?: "未知城市"
            println("执行get_weather工具，城市: $city")
            
            // 模拟天气数据
            val weatherData = getSimulatedWeatherData(city)
            
            // 格式化返回结果
            """
            |城市: $city
            |温度: ${weatherData.temperature}°C
            |天气: ${weatherData.condition}
            |湿度: ${weatherData.humidity}%
            |风速: ${weatherData.windSpeed} km/h
            |更新时间: ${weatherData.updateTime}
            """.trimMargin()
        }
    }
    
    // 添加天气预报工具
    tool {
        name = "get_forecast"
        description = "获取指定城市的天气预报"
        
        // 添加参数
        parameters {
            parameter {
                name = "city"
                description = "城市名称"
                type = "string"
                required = true
            }
            
            parameter {
                name = "days"
                description = "预报天数（1-7）"
                type = "number"
                required = false
            }
        }
        
        // 设置执行函数
        handler { params ->
            val city = params["city"] as? String ?: "未知城市"
            val days = (params["days"] as? Number)?.toInt() ?: 3
            val limitedDays = days.coerceIn(1, 7)
            
            println("执行get_forecast工具，城市: $city, 天数: $limitedDays")
            
            // 模拟天气预报数据
            val forecastData = getSimulatedForecastData(city, limitedDays)
            
            // 格式化返回结果
            val forecastText = forecastData.joinToString("\n\n") { forecast ->
                """
                |日期: ${forecast.date}
                |温度: ${forecast.temperature}°C
                |天气: ${forecast.condition}
                |湿度: ${forecast.humidity}%
                |风速: ${forecast.windSpeed} km/h
                """.trimMargin()
            }
            
            """
            |城市: $city
            |天气预报 ($limitedDays 天):
            |
            |$forecastText
            """.trimMargin()
        }
    }
}

/**
 * 创建天气查询 MCP 客户端
 */
private fun createWeatherClient() = mcpClient {
    name("WeatherMCPClient")
    version("1.0.0")
    
    server {
        sse {
            url = "http://localhost:8081"
        }
    }
}

/**
 * 天气数据类
 */
private data class WeatherData(
    val city: String,
    val temperature: Int,
    val condition: String,
    val humidity: Int,
    val windSpeed: Int,
    val updateTime: String
)

/**
 * 天气预报数据类
 */
private data class ForecastData(
    val date: String,
    val temperature: Int,
    val condition: String,
    val humidity: Int,
    val windSpeed: Int
)

/**
 * 获取模拟的天气数据
 */
private fun getSimulatedWeatherData(city: String): WeatherData {
    // 根据城市名生成一个伪随机数，使得同一城市每次返回相同结果
    val seed = city.hashCode()
    val random = java.util.Random(seed.toLong())
    
    // 生成随机天气数据
    val temperature = 10 + random.nextInt(25)
    val humidity = 30 + random.nextInt(50)
    val windSpeed = 5 + random.nextInt(20)
    
    // 根据温度选择天气状况
    val condition = when {
        temperature > 30 -> "晴朗"
        temperature > 25 -> "多云"
        temperature > 20 -> "阴天"
        temperature > 15 -> "小雨"
        temperature > 10 -> "雨天"
        temperature > 5 -> "雨夹雪"
        else -> "雪天"
    }
    
    // 获取当前时间
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val updateTime = LocalDateTime.now().format(formatter)
    
    return WeatherData(city, temperature, condition, humidity, windSpeed, updateTime)
}

/**
 * 获取模拟的天气预报数据
 */
private fun getSimulatedForecastData(city: String, days: Int): List<ForecastData> {
    // 根据城市名生成一个伪随机数，使得同一城市每次返回相同结果
    val seed = city.hashCode()
    val random = java.util.Random(seed.toLong())
    
    // 生成多天的天气预报
    return (0 until days).map { dayOffset ->
        // 生成随机天气数据，但保持一定的连续性
        val baseTemperature = 10 + random.nextInt(25)
        val temperature = baseTemperature + (-2 + random.nextInt(5))
        val humidity = 30 + random.nextInt(50)
        val windSpeed = 5 + random.nextInt(20)
        
        // 根据温度选择天气状况
        val condition = when {
            temperature > 30 -> "晴朗"
            temperature > 25 -> "多云"
            temperature > 20 -> "阴天"
            temperature > 15 -> "小雨"
            temperature > 10 -> "雨天"
            temperature > 5 -> "雨夹雪"
            else -> "雪天"
        }
        
        // 计算日期
        val date = LocalDateTime.now().plusDays(dayOffset.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        ForecastData(date, temperature, condition, humidity, windSpeed)
    }
}
