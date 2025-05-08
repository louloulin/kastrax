package ai.kastrax.examples.tools

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 用户数据类
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val birthDate: LocalDate,
    val isActive: Boolean,
    val tags: List<String> = emptyList()
)

/**
 * 用户验证结果数据类
 */
data class UserValidationResult(
    val isValid: Boolean,
    val user: User?,
    val errors: List<String> = emptyList()
)

/**
 * Zod 数据类工具示例
 */
fun main() = runBlocking {
    println("Zod 数据类工具示例")
    println("=================")

    // 创建用户验证工具
    val userValidationTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "validate_user"
        name = "用户验证"
        description = "验证用户数据并返回验证结果"

        // 定义输入模式
        inputSchema = obj {
            field("id", string())
            field("name", string { minLength = 2 })
            field("email", string { email = true })
            field("age", number { min = 18.0; max = 120.0 })
            field("birthDate", string())
            field("isActive", boolean())
            field("tags", array(string()), required = false)
        }

        // 定义输出模式
        outputSchema = obj {
            field("isValid", boolean())
            field("user", obj {
                field("id", string())
                field("name", string())
                field("email", string())
                field("age", number())
                field("birthDate", string())
                field("isActive", boolean())
                field("tags", array(string()))
            }, required = false)
            field("errors", array(string()), required = false)
        }

        // 实现执行逻辑
        execute = { input ->
            try {
                // 解析生日
                val birthDateStr = input["birthDate"] as String
                val birthDate = LocalDate.parse(birthDateStr)

                // 创建用户对象
                val user = mapOf(
                    "id" to input["id"],
                    "name" to input["name"],
                    "email" to input["email"],
                    "age" to input["age"],
                    "birthDate" to birthDateStr,
                    "isActive" to input["isActive"],
                    "tags" to (input["tags"] ?: emptyList<String>())
                )

                // 执行额外的业务逻辑验证
                val errors = mutableListOf<String>()

                // 检查年龄与生日是否匹配
                val calculatedAge = LocalDate.now().year - birthDate.year
                val inputAge = (input["age"] as Number).toInt()
                if (calculatedAge != inputAge) {
                    errors.add("年龄与生日不匹配")
                }

                // 返回验证结果
                if (errors.isEmpty()) {
                    mapOf(
                        "isValid" to true,
                        "user" to user,
                        "errors" to emptyList<String>()
                    )
                } else {
                    mapOf(
                        "isValid" to false,
                        "user" to user,
                        "errors" to errors
                    )
                }
            } catch (e: Exception) {
                mapOf(
                    "isValid" to false,
                    "errors" to listOf("验证失败: ${e.message}")
                )
            }
        }
    }

    // 测试用户验证工具
    println("\n测试有效用户:")
    val validUserInput = mapOf(
        "id" to "user123",
        "name" to "张三",
        "email" to "zhangsan@example.com",
        "age" to 30,
        "birthDate" to "1993-05-15",
        "isActive" to true,
        "tags" to listOf("vip", "premium")
    )

    val validResult = userValidationTool.execute(validUserInput)
    println("验证结果: ${if (validResult["isValid"] == true) "有效" else "无效"}")
    println("用户信息: ${validResult["user"]}")
    val validErrors = validResult["errors"] as? List<*>
    if (validErrors?.isNotEmpty() == true) {
        println("错误信息: $validErrors")
    }

    println("\n测试无效用户 (年龄与生日不匹配):")
    val invalidUserInput = mapOf(
        "id" to "user456",
        "name" to "李四",
        "email" to "lisi@example.com",
        "age" to 25, // 与生日不匹配
        "birthDate" to "1990-08-20",
        "isActive" to true
    )

    val invalidResult = userValidationTool.execute(invalidUserInput)
    println("验证结果: ${if (invalidResult["isValid"] == true) "有效" else "无效"}")
    println("用户信息: ${invalidResult["user"]}")
    val invalidErrors = invalidResult["errors"] as? List<*>
    if (invalidErrors?.isNotEmpty() == true) {
        println("错误信息: $invalidErrors")
    }

    println("\nZod 数据类工具示例完成")
}
