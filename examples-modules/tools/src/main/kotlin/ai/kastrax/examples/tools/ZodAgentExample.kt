package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.zodTool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

/**
 * 天气查询请求
 */
data class WeatherRequest(
    val location: String,
    val unit: String = "celsius"
)

/**
 * 天气查询结果
 */
data class WeatherResponse(
    val location: String,
    val temperature: Double,
    val conditions: String,
    val humidity: Int,
    val windSpeed: Double,
    val unit: String
)

/**
 * 货币转换请求
 */
data class CurrencyConversionRequest(
    val amount: Double,
    val fromCurrency: String,
    val toCurrency: String
)

/**
 * 货币转换结果
 */
data class CurrencyConversionResponse(
    val amount: Double,
    val fromCurrency: String,
    val toCurrency: String,
    val convertedAmount: Double,
    val rate: Double
)

/**
 * Zod 代理工具示例
 */
fun main() = runBlocking {
    println("Zod 代理工具示例")
    println("===============")

    // 创建天气工具
    val weatherTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "get_weather"
        name = "获取天气"
        description = "获取指定位置的天气信息"

        // 定义输入模式
        inputSchema = obj {
            field("location", string { minLength = 1 })
            field("unit", string {
                enum("celsius", "fahrenheit")
            }, required = false)
        }

        // 定义输出模式
        outputSchema = obj {
            field("location", string())
            field("temperature", number())
            field("conditions", string())
            field("humidity", number { int() })
            field("windSpeed", number())
            field("unit", string())
        }

        // 实现执行逻辑
        execute = { input ->
            // 解析输入
            val location = input["location"] as String
            val unit = input["unit"] as? String ?: "celsius"

            // 模拟天气 API 调用
            println("获取 $location 的天气信息，单位: $unit")

            // 生成模拟天气数据
            val baseTemp = 22.5
            val temp = if (unit == "fahrenheit") baseTemp * 9/5 + 32 else baseTemp

            mapOf(
                "location" to location,
                "temperature" to temp,
                "conditions" to "晴朗",
                "humidity" to 65,
                "windSpeed" to 10.5,
                "unit" to unit
            )
        }
    }

    // 创建货币转换工具
    val currencyTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "convert_currency"
        name = "货币转换"
        description = "将一种货币转换为另一种货币"

        // 定义输入模式
        inputSchema = obj {
            field("amount", number { min = 0.0 })
            field("fromCurrency", string {
                enum("CNY", "USD", "EUR", "JPY", "GBP")
            })
            field("toCurrency", string {
                enum("CNY", "USD", "EUR", "JPY", "GBP")
            })
        }

        // 定义输出模式
        outputSchema = obj {
            field("amount", number())
            field("fromCurrency", string())
            field("toCurrency", string())
            field("convertedAmount", number())
            field("rate", number())
        }

        // 实现执行逻辑
        execute = { input ->
            // 解析输入
            val amount = (input["amount"] as Number).toDouble()
            val fromCurrency = input["fromCurrency"] as String
            val toCurrency = input["toCurrency"] as String

            // 模拟汇率数据
            val rates = mapOf(
                "CNY" to 1.0,
                "USD" to 7.2,
                "EUR" to 7.8,
                "JPY" to 0.05,
                "GBP" to 9.1
            )

            // 计算转换率
            val fromRate = rates[fromCurrency] ?: 1.0
            val toRate = rates[toCurrency] ?: 1.0
            val rate = toRate / fromRate

            // 计算转换后的金额
            val convertedAmount = amount * rate

            println("转换 $amount $fromCurrency 到 $toCurrency")
            println("汇率: $rate")

            mapOf(
                "amount" to amount,
                "fromCurrency" to fromCurrency,
                "toCurrency" to toCurrency,
                "convertedAmount" to convertedAmount,
                "rate" to rate
            )
        }
    }

    // 创建使用这些工具的代理
    val myAgent = agent {
        name = "助手"
        instructions = """
            你是一个有用的助手，可以提供天气信息和货币转换服务。
            使用提供的工具来回答用户的问题。
        """.trimIndent()

        // 使用 Deepseek 模型
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
        }

        // 添加工具
        tools {
            tool(weatherTool.toTool())
            tool(currencyTool.toTool())
        }
    }

    // 测试代理
    println("\n测试代理:")
    val queries = listOf(
        "北京今天的天气怎么样？",
        "请将100美元转换为人民币",
        "伦敦的天气如何？请用华氏度显示",
        "500欧元等于多少英镑？"
    )

    for (query in queries) {
        println("\n用户问题: $query")
        val response = myAgent.generate(query)
        println("代理回答: ${response.text}")
    }

    println("\nZod 代理工具示例完成")
}
