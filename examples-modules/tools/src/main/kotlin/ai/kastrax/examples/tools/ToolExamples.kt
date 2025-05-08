package ai.kastrax.examples.tools

/**
 * 工具示例入口类
 */
fun main(args: Array<String>) {
    println("=== 工具示例 ===")
    
    if (args.isEmpty()) {
        println("可用的工具示例:")
        println("1. advanced-zod - 运行高级Zod工具示例")
        println("2. data-class - 运行数据类Zod工具示例")
        println("3. date-time - 运行日期时间工具示例")
        println("4. zod-advanced - 运行Zod高级工具示例")
        println("5. calculator - 运行Zod计算器示例")
        println("6. calculator-tool - 运行Zod计算器工具示例")
        println("7. all - 运行所有工具示例")
        return
    }
    
    when (args[0]) {
        "advanced-zod" -> runAdvancedZodToolExample()
        "data-class" -> runDataClassZodToolExample()
        "date-time" -> runDateTimeToolExample()
        "zod-advanced" -> runZodAdvancedToolExample()
        "calculator" -> runZodCalculatorExample()
        "calculator-tool" -> runZodCalculatorToolExample()
        "all" -> runAllExamples()
        else -> {
            println("未知示例: ${args[0]}")
            println("请提供有效的示例名称")
        }
    }
}

/**
 * 运行高级Zod工具示例
 */
fun runAdvancedZodToolExample() {
    println("运行高级Zod工具示例...")
    try {
        // 这里将调用AdvancedZodToolExample.kt中的main函数
        println("高级Zod工具示例实现了高级Zod工具，包括复杂数据结构的验证和转换。")
    } catch (e: Exception) {
        println("运行高级Zod工具示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行数据类Zod工具示例
 */
fun runDataClassZodToolExample() {
    println("运行数据类Zod工具示例...")
    try {
        // 这里将调用DataClassZodToolExample.kt中的main函数
        println("数据类Zod工具示例实现了使用数据类的Zod工具，包括用户数据验证。")
    } catch (e: Exception) {
        println("运行数据类Zod工具示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行日期时间工具示例
 */
fun runDateTimeToolExample() {
    println("运行日期时间工具示例...")
    try {
        // 这里将调用DateTimeToolExample.kt中的main函数
        println("日期时间工具示例实现了日期时间工具，包括获取当前时间、格式化、解析、加减和时区转换等功能。")
    } catch (e: Exception) {
        println("运行日期时间工具示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Zod高级工具示例
 */
fun runZodAdvancedToolExample() {
    println("运行Zod高级工具示例...")
    try {
        // 这里将调用ZodAdvancedToolExample.kt中的main函数
        println("Zod高级工具示例实现了高级Zod工具，包括用户搜索功能和复杂数据结构处理。")
    } catch (e: Exception) {
        println("运行Zod高级工具示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Zod计算器示例
 */
fun runZodCalculatorExample() {
    println("运行Zod计算器示例...")
    try {
        // 这里将调用ZodCalculatorExample.kt中的main函数
        println("Zod计算器示例实现了计算器工具，可以执行基本的数学运算。")
    } catch (e: Exception) {
        println("运行Zod计算器示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Zod计算器工具示例
 */
fun runZodCalculatorToolExample() {
    println("运行Zod计算器工具示例...")
    try {
        // 这里将调用ZodCalculatorToolExample.kt中的main函数
        println("Zod计算器工具示例实现了使用数据类的计算器工具，包括输入验证和结果格式化。")
    } catch (e: Exception) {
        println("运行Zod计算器工具示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行所有工具示例
 */
fun runAllExamples() {
    println("运行所有工具示例...")
    runAdvancedZodToolExample()
    runDataClassZodToolExample()
    runDateTimeToolExample()
    runZodAdvancedToolExample()
    runZodCalculatorExample()
    runZodCalculatorToolExample()
}
