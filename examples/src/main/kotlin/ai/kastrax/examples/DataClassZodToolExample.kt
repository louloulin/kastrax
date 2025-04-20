package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

/**
 * 用户数据类。
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int
)

/**
 * 用户验证结果数据类。
 */
data class UserValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * 使用数据类的 ZodTool 示例。
 */
fun main() {
    // 创建用户验证工具
    val userValidatorTool = zodTool<User, UserValidationResult> {
        id = "user_validator"
        name = "User Validator"
        description = "Validates user data"
        
        // 创建输入模式
        val userSchema = objectInput("User data") {
            stringField("id", "User ID") {
                minLength = 3
                maxLength = 50
            }
            stringField("name", "User name") {
                minLength = 2
                maxLength = 100
            }
            stringField("email", "User email") {
                email = true
            }
            numberField("age", "User age") {
                min = 0.0
                max = 120.0
            }
        }.transform { input ->
            User(
                id = input["id"] as String,
                name = input["name"] as String,
                email = input["email"] as String,
                age = (input["age"] as Number).toInt()
            )
        }
        
        // 创建输出模式
        val resultSchema = objectOutput("Validation result") {
            booleanField("isValid", "Whether the user data is valid")
            arrayField("errors", stringInput("Error message"), "List of validation errors")
        }.transform { output ->
            UserValidationResult(
                isValid = output["isValid"] as Boolean,
                errors = output["errors"] as List<String>
            )
        }
        
        inputSchema = userSchema
        outputSchema = resultSchema
        
        // 实现执行逻辑
        execute = { user ->
            val errors = mutableListOf<String>()
            
            // 验证 ID
            if (!user.id.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                errors.add("ID can only contain letters, numbers, underscores, and hyphens")
            }
            
            // 验证名称
            if (user.name.isBlank()) {
                errors.add("Name cannot be blank")
            }
            
            // 验证邮箱
            if (!user.email.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
                errors.add("Invalid email format")
            }
            
            // 验证年龄
            if (user.age < 18) {
                errors.add("User must be at least 18 years old")
            }
            
            UserValidationResult(
                isValid = errors.isEmpty(),
                errors = errors
            )
        }
    }
    
    // 使用工具
    val validUser = User(
        id = "user123",
        name = "John Doe",
        email = "john.doe@example.com",
        age = 30
    )
    
    val invalidUser = User(
        id = "user@123", // 包含无效字符
        name = "",       // 名称为空
        email = "invalid-email", // 无效邮箱
        age = 15         // 年龄小于 18
    )
    
    // 验证有效用户
    println("Validating valid user:")
    val validResult = runBlocking {
        userValidatorTool.execute(validUser)
    }
    
    println("Is valid: ${validResult.isValid}")
    if (!validResult.isValid) {
        println("Errors: ${validResult.errors.joinToString(", ")}")
    }
    
    // 验证无效用户
    println("\nValidating invalid user:")
    val invalidResult = runBlocking {
        userValidatorTool.execute(invalidUser)
    }
    
    println("Is valid: ${invalidResult.isValid}")
    if (!invalidResult.isValid) {
        println("Errors: ${invalidResult.errors.joinToString(", ")}")
    }
}
