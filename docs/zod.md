# KastraX Zod: Kotlin 版本的 Zod 实现计划

## 概述

KastraX Zod 是一个受 TypeScript 的 [Zod](https://github.com/colinhacks/zod) 库启发的 Kotlin 实现，旨在为 KastraX 框架提供类型安全的模式验证系统。该库主要用于验证工具的输入和输出，但也可以扩展到框架的其他部分。

## 目标

1. 提供类型安全的模式验证
2. 创建符合 Kotlin 风格的 DSL
3. 支持所有主要的 Zod 功能
4. 与 KastraX 工具系统无缝集成
5. 提供清晰的错误消息和格式化

## 目录结构

```
kastrax/
└── kastrax-zod/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   └── kotlin/
        │       └── ai/
        │           └── kastrax/
        │               └── zod/
        │                   ├── Schema.kt           # 基础模式接口和通用功能
        │                   ├── SchemaTypes.kt      # 基本模式类型 (字符串, 数字, 布尔等)
        │                   ├── SchemaObjects.kt    # 对象模式实现
        │                   ├── SchemaArrays.kt     # 数组模式实现
        │                   ├── SchemaUnions.kt     # 联合和交叉模式实现
        │                   ├── SchemaRefinement.kt # 细化和转换功能
        │                   ├── SchemaError.kt      # 错误处理和格式化
        │                   └── SchemaDsl.kt        # 创建模式的 DSL
        └── test/
            └── kotlin/
                └── ai/
                    └── kastrax/
                        └── zod/
                            ├── SchemaTest.kt
                            ├── SchemaTypesTest.kt
                            ├── SchemaObjectsTest.kt
                            ├── SchemaArraysTest.kt
                            ├── SchemaUnionsTest.kt
                            ├── SchemaRefinementTest.kt
                            └── SchemaErrorTest.kt
```

## 详细实现计划

### 1. 创建基础模式接口和类型

**Schema.kt** ✅
- ✅ 定义基础 `Schema<I, O>` 接口，带有泛型输入和输出类型
- ✅ 实现核心验证方法：`parse`, `safeParse`, `parseAsync`, `safeParseAsync`
- ✅ 定义错误处理结构
- ✅ 实现通用模式操作（可选、可空等）

**SchemaTypes.kt** ✅
- ✅ 实现基本模式类型：
  - ✅ `StringSchema`：字符串验证，支持最小/最大长度、模式等
  - ✅ `NumberSchema`：数字验证，支持最小/最大值、整数检查等
  - ✅ `BooleanSchema`：布尔验证
  - ✅ `EnumSchema`：枚举验证
  - ✅ `LiteralSchema`：字面值验证
  - ✅ `NullSchema`：null 验证
  - ✅ `UndefinedSchema`：未定义验证
  - ✅ `AnySchema`：任意类型验证

### 2. 实现复杂模式类型

**SchemaObjects.kt** ✅
- ✅ 实现 `ObjectSchema` 用于验证对象结构
- ✅ 支持必填和可选字段
- ✅ 提供扩展、合并、选择和忽略字段的方法
- ✅ 支持深度部分验证和严格验证选项

**SchemaArrays.kt** ✅
- ✅ 实现 `ArraySchema` 用于验证数组
- ✅ 支持元素验证、最小/最大长度等
- ✅ 实现 `TupleSchema` 用于验证固定长度且元素类型不同的数组

**SchemaUnions.kt** ✅
- ✅ 实现 `UnionSchema` 用于验证联合类型（OR）
- ✅ 实现 `IntersectionSchema` 用于验证交叉类型（AND）
- ✅ 支持可区分联合类型

### 3. 实现细化和转换功能

**SchemaRefinement.kt** ✅
- ✅ 实现 `refine` 方法用于添加自定义验证逻辑
- ✅ 实现 `transform` 方法用于转换已验证的数据
- ✅ 支持异步细化和转换
- ✅ 实现管道功能用于链接模式

### 4. 错误处理和格式化

**SchemaError.kt** ✅
- ✅ 定义错误类型和结构
- ✅ 实现错误格式化，提供用户友好的消息
- ✅ 支持自定义错误映射和国际化

### 5. 创建模式创建的 DSL

**SchemaDsl.kt** ✅
- ✅ 实现 Kotlin DSL 用于创建模式
- ✅ 使 API 在 Kotlin 中感觉自然
- ✅ 支持所有模式类型和操作

### 6. 与工具系统集成

- 更新 `Tool` 接口和 `ToolBuilder` 以使用新的模式系统
- 创建常见工具模式模式的辅助函数
- 确保与现有工具的向后兼容性

## 使用示例

```kotlin
// 定义用户模式
val userSchema = obj {
    field("name", string {
        minLength = 2
        maxLength = 50
    })
    field("age", number {
        int = true
        minimum = 0
    })
    field("email", string {
        email = true
    })
    field("role", enum("admin", "user", "guest"))
}

// 解析和验证数据
val result = userSchema.safeParse(jsonData)
if (result.success) {
    val user = result.data
    println("有效用户: ${user.getString("name")}")
} else {
    println("验证错误: ${result.error.format()}")
}

// 在工具中使用
val userTool = tool {
    id = "user-tool"
    name = "用户管理"
    description = "管理用户数据"

    // 使用新系统定义输入模式
    input {
        obj {
            field("action", string {
                enum("create", "update", "delete")
            })
            field("userData", userSchema)
        }
    }

    // 定义输出模式
    output {
        obj {
            field("success", boolean())
            field("message", string())
            field("user", userSchema.optional())
        }
    }

    // 执行，使用类型安全的方式访问已验证的数据
    execute { input ->
        val action = input.getString("action")
        val userData = input.getObject("userData")

        // 处理数据...

        output {
            "success" to true
            "message" to "用户处理成功"
            "user" to userData
        }
    }
}
```

## 实现时间线

1. **阶段 1：核心实现（1-2 周）** ✅
   - ✅ 实现基础 Schema 接口
   - ✅ 实现基本模式类型
   - ✅ 基本错误处理

2. **阶段 2：复杂类型（1-2 周）** ✅
   - ✅ 实现对象模式
   - ✅ 实现数组和元组模式
   - ✅ 实现联合和交叉模式

3. **阶段 3：细化和转换（1 周）** ✅
   - ✅ 实现细化功能
   - ✅ 实现转换功能
   - ✅ 实现管道功能

4. **阶段 4：DSL 和集成（1-2 周）** ✅
   - ✅ 创建 Kotlin DSL 用于模式创建
   - ❌ 与工具系统集成 (待完成)
   - ✅ 编写文档和示例

5. **阶段 5：测试和优化（1 周）** ✅
   - ✅ 编写测试
   - ✅ 测试通过
   - ❌ 优化性能 (待完成)
   - ✅ 完成文档

## 挑战和考虑因素

1. **类型安全**：在没有 TypeScript 结构化类型的情况下确保 Kotlin 中的类型安全
2. **性能**：优化运行时验证性能
3. **错误消息**：创建清晰有用的错误消息
4. **向后兼容性**：确保与现有代码的兼容性
5. **异步支持**：正确处理异步验证和转换

## 与 TypeScript Zod 的比较

| 特性 | TypeScript Zod | KastraX Zod |
|------|---------------|-------------|
| 语法 | 函数链式调用 | Kotlin DSL |
| 类型安全 | 通过 TypeScript 类型推断 | 通过 Kotlin 类型系统 |
| 验证时机 | 运行时 | 运行时 |
| 错误处理 | 错误对象 | 结果类型和异常 |
| 自定义验证 | 支持 | 支持 |
| 类型转换 | 支持 | 支持 |
| 默认值 | 支持 | 支持 |
| 集成 | 与 TypeScript 生态系统集成 | 与 Kotlin 和 JVM 生态系统集成 |

## 当前状态和下一步

### 当前状态

KastraX Zod 的核心功能已经实现，包括：

- ✅ 基础模式接口和类型
- ✅ 基本模式类型（字符串、数字、布尔等）
- ✅ 复杂模式类型（对象、数组、元组、联合等）
- ✅ 细化和转换功能
- ✅ 错误处理和格式化
- ✅ Kotlin DSL 用于模式创建

然而，当前还存在一些待完成的任务：

- ✅ 测试已全部通过
- ❌ 类型安全问题：存在一些类型转换和类型推断的问题，需要进一步优化
- ❌ 与工具系统的集成尚未完成
- ❌ 性能优化尚未进行

### 下一步

1. **优化类型安全**
   - 改进泛型类型参数的使用
   - 减少类型转换的需要
   - 增强类型推断
   - 减少警告和类型转换注解

2. **与工具系统集成**
   - 更新 `Tool` 接口和 `ToolBuilder` 以使用新的模式系统
   - 创建常见工具模式的辅助函数
   - 确保与现有工具的向后兼容性
   - 添加集成示例和文档

3. **性能优化**
   - 分析并优化验证性能
   - 减少内存占用
   - 优化错误处理
   - 添加性能基准测试

4. **扩展功能**
   - 添加更多高级特性，如递归类型、延迟加载等
   - 添加更多验证器和转换器
   - 添加更多实用工具和辅助函数

## 结论

KastraX Zod 将为 KastraX 框架提供强大的类型安全模式验证系统，使开发人员能够更轻松地创建健壮可靠的工具和应用程序。通过采用 Kotlin 的强大特性和 DSL 功能，我们可以创建一个既类型安全又易于使用的 API。

目前的实现已经完成了所有核心功能，并且所有测试都已经通过。下一步将专注于优化类型安全、完成与工具系统的集成、性能优化以及扩展更多功能。

KastraX Zod 已经准备好了为 KastraX 框架提供强大的数据验证能力，并将成为框架中不可或缺的组成部分。
