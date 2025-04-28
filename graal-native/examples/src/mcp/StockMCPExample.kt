package ai.kastrax.examples.mcp

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 股票行情 MCP 应用案例
 * 
 * 这个示例展示了如何创建一个提供股票行情查询功能的 MCP 服务器，
 * 以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。
 */
fun main() = runBlocking {
    println("启动股票行情 MCP 应用案例...")
    
    // 启动服务器
    val serverJob = launch {
        val server = createStockServer()
        server.startSSE(port = 8082)
        println("股票行情 MCP 服务器已启动在端口 8082")
        
        // 保持服务器运行
        try {
            while (true) {
                delay(1000)
            }
        } finally {
            server.stop()
            println("股票行情 MCP 服务器已停止")
        }
    }
    
    // 等待服务器启动
    delay(2000)
    
    // 创建并使用客户端
    val client = createStockClient()
    
    try {
        // 连接到服务器
        println("连接到股票行情 MCP 服务器...")
        client.connect()
        println("已连接到股票行情 MCP 服务器")
        
        // 获取服务器能力
        val hasTools = client.supportsCapability("tools")
        println("服务器支持工具: $hasTools")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 查询不同股票的行情
        val stocks = listOf(
            "AAPL" to "苹果公司",
            "MSFT" to "微软公司",
            "GOOGL" to "谷歌公司",
            "AMZN" to "亚马逊公司",
            "TSLA" to "特斯拉公司"
        )
        
        for ((symbol, name) in stocks) {
            println("\n查询${name}(${symbol})的股票行情...")
            val stockResult = client.callTool("get_stock_price", mapOf("symbol" to symbol))
            println("${name}(${symbol})股票行情: $stockResult")
        }
        
        // 查询股票历史数据
        println("\n查询苹果公司(AAPL)的历史股价...")
        val historyResult = client.callTool("get_stock_history", mapOf("symbol" to "AAPL", "days" to 5))
        println("苹果公司(AAPL)历史股价: $historyResult")
        
        // 查询市场概览
        println("\n查询市场概览...")
        val marketResult = client.callTool("get_market_overview", emptyMap())
        println("市场概览: $marketResult")
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与股票行情 MCP 服务器的连接")
        
        // 停止服务器
        serverJob.cancel()
    }
}

/**
 * 创建股票行情 MCP 服务器
 */
private fun createStockServer() = mcpServer {
    name("StockMCPServer")
    version("1.0.0")
    
    // 添加股票价格查询工具
    tool {
        name = "get_stock_price"
        description = "获取指定股票的当前价格"
        
        // 添加参数
        parameters {
            parameter {
                name = "symbol"
                description = "股票代码"
                type = "string"
                required = true
            }
        }
        
        // 设置执行函数
        handler { params ->
            val symbol = params["symbol"] as? String ?: "未知股票"
            println("执行get_stock_price工具，股票代码: $symbol")
            
            // 模拟股票数据
            val stockData = getSimulatedStockData(symbol)
            
            // 格式化返回结果
            """
            |股票代码: ${stockData.symbol}
            |公司名称: ${stockData.companyName}
            |当前价格: $${stockData.price}
            |涨跌幅: ${stockData.changePercent}%
            |成交量: ${stockData.volume}
            |更新时间: ${stockData.updateTime}
            """.trimMargin()
        }
    }
    
    // 添加股票历史数据查询工具
    tool {
        name = "get_stock_history"
        description = "获取指定股票的历史价格数据"
        
        // 添加参数
        parameters {
            parameter {
                name = "symbol"
                description = "股票代码"
                type = "string"
                required = true
            }
            
            parameter {
                name = "days"
                description = "历史天数（1-30）"
                type = "number"
                required = false
            }
        }
        
        // 设置执行函数
        handler { params ->
            val symbol = params["symbol"] as? String ?: "未知股票"
            val days = (params["days"] as? Number)?.toInt() ?: 7
            val limitedDays = days.coerceIn(1, 30)
            
            println("执行get_stock_history工具，股票代码: $symbol, 天数: $limitedDays")
            
            // 模拟股票历史数据
            val historyData = getSimulatedStockHistory(symbol, limitedDays)
            
            // 获取公司名称
            val companyName = getCompanyName(symbol)
            
            // 格式化返回结果
            val historyText = historyData.joinToString("\n") { history ->
                "${history.date}: $${history.price} (${history.changePercent}%)"
            }
            
            """
            |股票代码: $symbol
            |公司名称: $companyName
            |历史价格数据 ($limitedDays 天):
            |
            |$historyText
            """.trimMargin()
        }
    }
    
    // 添加市场概览工具
    tool {
        name = "get_market_overview"
        description = "获取当前市场概览"
        
        // 设置执行函数
        handler { _ ->
            println("执行get_market_overview工具")
            
            // 模拟市场数据
            val marketData = getSimulatedMarketData()
            
            // 格式化返回结果
            """
            |市场概览:
            |
            |道琼斯工业平均指数: ${marketData.dow} (${marketData.dowChange}%)
            |纳斯达克综合指数: ${marketData.nasdaq} (${marketData.nasdaqChange}%)
            |标普500指数: ${marketData.sp500} (${marketData.sp500Change}%)
            |
            |交易量: ${marketData.volume}
            |上涨股票数: ${marketData.advancers}
            |下跌股票数: ${marketData.decliners}
            |
            |更新时间: ${marketData.updateTime}
            """.trimMargin()
        }
    }
}

/**
 * 创建股票行情 MCP 客户端
 */
private fun createStockClient() = mcpClient {
    name("StockMCPClient")
    version("1.0.0")
    
    server {
        sse {
            url = "http://localhost:8082"
        }
    }
}

/**
 * 股票数据类
 */
private data class StockData(
    val symbol: String,
    val companyName: String,
    val price: Double,
    val changePercent: Double,
    val volume: Long,
    val updateTime: String
)

/**
 * 股票历史数据类
 */
private data class StockHistoryData(
    val date: String,
    val price: Double,
    val changePercent: Double
)

/**
 * 市场数据类
 */
private data class MarketData(
    val dow: Double,
    val dowChange: Double,
    val nasdaq: Double,
    val nasdaqChange: Double,
    val sp500: Double,
    val sp500Change: Double,
    val volume: Long,
    val advancers: Int,
    val decliners: Int,
    val updateTime: String
)

/**
 * 获取公司名称
 */
private fun getCompanyName(symbol: String): String {
    return when (symbol.uppercase()) {
        "AAPL" -> "苹果公司"
        "MSFT" -> "微软公司"
        "GOOGL" -> "谷歌公司"
        "AMZN" -> "亚马逊公司"
        "TSLA" -> "特斯拉公司"
        "META" -> "Meta公司"
        "NFLX" -> "奈飞公司"
        "NVDA" -> "英伟达公司"
        "INTC" -> "英特尔公司"
        "AMD" -> "超微半导体公司"
        else -> "$symbol 公司"
    }
}

/**
 * 获取模拟的股票数据
 */
private fun getSimulatedStockData(symbol: String): StockData {
    // 根据股票代码生成一个伪随机数，使得同一股票每次返回相似结果
    val seed = symbol.hashCode()
    val random = Random(seed)
    
    // 生成基础价格（根据股票代码）
    val basePrice = when (symbol.uppercase()) {
        "AAPL" -> 150.0
        "MSFT" -> 300.0
        "GOOGL" -> 120.0
        "AMZN" -> 130.0
        "TSLA" -> 250.0
        "META" -> 300.0
        "NFLX" -> 400.0
        "NVDA" -> 450.0
        "INTC" -> 35.0
        "AMD" -> 100.0
        else -> 100.0
    }
    
    // 添加一些随机波动
    val priceVariation = basePrice * 0.05 * (random.nextDouble() - 0.5)
    val price = (basePrice + priceVariation).round(2)
    
    // 生成涨跌幅
    val changePercent = (random.nextDouble() * 6 - 3).round(2)
    
    // 生成成交量
    val volume = 1_000_000L + random.nextLong(9_000_000L)
    
    // 获取当前时间
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val updateTime = LocalDateTime.now().format(formatter)
    
    return StockData(
        symbol = symbol.uppercase(),
        companyName = getCompanyName(symbol),
        price = price,
        changePercent = changePercent,
        volume = volume,
        updateTime = updateTime
    )
}

/**
 * 获取模拟的股票历史数据
 */
private fun getSimulatedStockHistory(symbol: String, days: Int): List<StockHistoryData> {
    // 根据股票代码生成一个伪随机数，使得同一股票每次返回相似结果
    val seed = symbol.hashCode()
    val random = Random(seed)
    
    // 生成基础价格（根据股票代码）
    val basePrice = when (symbol.uppercase()) {
        "AAPL" -> 150.0
        "MSFT" -> 300.0
        "GOOGL" -> 120.0
        "AMZN" -> 130.0
        "TSLA" -> 250.0
        "META" -> 300.0
        "NFLX" -> 400.0
        "NVDA" -> 450.0
        "INTC" -> 35.0
        "AMD" -> 100.0
        else -> 100.0
    }
    
    // 生成历史数据
    var currentPrice = basePrice
    return (days downTo 1).map { daysAgo ->
        // 添加一些随机波动，但保持一定的连续性
        val priceChange = currentPrice * 0.03 * (random.nextDouble() - 0.5)
        currentPrice = (currentPrice + priceChange).round(2)
        
        // 生成涨跌幅
        val changePercent = (random.nextDouble() * 4 - 2).round(2)
        
        // 计算日期
        val date = LocalDateTime.now().minusDays(daysAgo.toLong()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        StockHistoryData(date, currentPrice, changePercent)
    }.reversed() // 反转列表，使日期从早到晚排序
}

/**
 * 获取模拟的市场数据
 */
private fun getSimulatedMarketData(): MarketData {
    // 使用当前时间作为种子，使得每次运行结果不同
    val random = Random(System.currentTimeMillis())
    
    // 生成指数数据
    val dow = 34000.0 + random.nextDouble(-500.0, 500.0)
    val dowChange = random.nextDouble(-2.0, 2.0)
    
    val nasdaq = 14000.0 + random.nextDouble(-300.0, 300.0)
    val nasdaqChange = random.nextDouble(-2.5, 2.5)
    
    val sp500 = 4300.0 + random.nextDouble(-100.0, 100.0)
    val sp500Change = random.nextDouble(-2.0, 2.0)
    
    // 生成交易量
    val volume = 5_000_000_000L + random.nextLong(3_000_000_000L)
    
    // 生成上涨/下跌股票数
    val total = 3000
    val advancers = if (dowChange > 0) {
        total / 2 + random.nextInt(total / 3)
    } else {
        total / 3 - random.nextInt(total / 6)
    }
    val decliners = total - advancers
    
    // 获取当前时间
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val updateTime = LocalDateTime.now().format(formatter)
    
    return MarketData(
        dow = dow.round(2),
        dowChange = dowChange.round(2),
        nasdaq = nasdaq.round(2),
        nasdaqChange = nasdaqChange.round(2),
        sp500 = sp500.round(2),
        sp500Change = sp500Change.round(2),
        volume = volume,
        advancers = advancers,
        decliners = decliners,
        updateTime = updateTime
    )
}

/**
 * 将Double四舍五入到指定小数位
 */
private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}
