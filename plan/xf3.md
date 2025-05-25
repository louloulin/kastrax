# Kastrax Examples 修复计划

## 1. 问题分析

根据之前的编译错误，我们发现了以下几类问题：

### 1.1 主要错误类型

1. **类重复声明**：多个文件中定义了相同名称的数据类（如 `User`、`UserSearchResult`、`CalculatorInput` 等）
2. **类型不匹配**：多处代码中存在类型不匹配问题，特别是在 Zod 相关的工具示例中
3. **未解析的引用**：`DataSourceExample.kt` 中有大量未解析的引用，如 `datasource`、`DataSourceManager` 等
4. **API 使用错误**：`WorkflowRetryExample.kt` 中存在 API 使用错误
5. **MemoryPriority 构造问题**：使用了浮点数构造器而不是枚举值
6. **变量重新赋值**：多个示例中尝试对 `val` 变量进行重新赋值
7. **中文引号问题**：在字符串中使用了中文引号，导致编译错误
8. **main 函数冲突**：多个文件中定义了相同签名的 `main()` 函数

### 1.2 受影响的文件

主要受影响的文件包括：

- Zod 相关示例：`AdvancedZodToolExample.kt`、`DataClassZodToolExample.kt`、`ZodAdvancedToolExample.kt` 等
- 数据源示例：`DataSourceExample.kt`
- 工作流示例：`WorkflowRetryExample.kt`、`EventCallbackWorkflowExample.kt` 等
- 内存相关示例：`MemoryManagerExample.kt`、`TagsAndSharingExample.kt`、`WorkingMemoryExample.kt`、`MemoryCompressionExample.kt`

## 2. 修复策略

### 2.1 短期修复策略

为了快速修复编译问题，我们采用以下策略：

1. **排除问题文件**：通过在 `build.gradle.kts` 中使用 `sourceSets` 配置，排除有问题的文件
2. **修复简单错误**：修复简单的语法错误，如中文引号、变量重新赋值等
3. **添加必要依赖**：确保 `examples` 模块依赖于所有必要的 Kastrax 模块

### 2.2 长期修复策略

对于更复杂的问题，我们需要：

1. **重构数据类**：为重复的数据类添加不同的命名空间或重命名
2. **更新 API 使用**：根据最新的 API 更新示例代码
3. **完善缺失的实现**：为 `DataSourceExample.kt` 等文件实现缺失的功能
4. **统一编码风格**：确保所有示例使用一致的编码风格和最佳实践

## 3. 已完成的修复

### 3.1 build.gradle.kts 修改

已经对 `examples/build.gradle.kts` 进行了以下修改：

1. 添加了 Kastrax 核心依赖：
   ```kotlin
   // Kastrax core dependencies
   add("implementation", project(":kastrax-core"))
   add("implementation", project(":kastrax-memory-api"))
   add("implementation", project(":kastrax-memory-impl"))
   add("implementation", project(":kastrax-zod"))
   add("implementation", project(":kastrax-rag"))
   add("implementation", project(":kastrax-integrations:kastrax-deepseek"))
   
   // Actor integration
   add("implementation", project(":kastrax-actor"))
   ```

2. 添加了 sourceSets 配置，排除有问题的文件：
   ```kotlin
   sourceSets {
       main {
           kotlin {
               // 只包含已修复的文件
               include(
                   "**/DeepSeekExample.kt",
                   "**/DeepSeekStreamingExample.kt",
                   // ... 其他已修复的文件
               )
               // 排除有问题的文件
               exclude(
                   "**/AdvancedWorkflowExample.kt",
                   "**/RAGExample.kt",
                   // ... 其他有问题的文件
               )
           }
       }
   }
   ```

### 3.2 内存相关示例修复

已经修复了以下内存相关示例：

1. **MemoryManagerExample.kt**：
   - 将 `MemoryPriority(0.8f)` 替换为 `MemoryPriority.HIGH`
   - 将 `MemoryPriority(0.9f)` 替换为 `MemoryPriority.VERY_HIGH`

2. **TagsAndSharingExample.kt**：
   - 修复了中文引号问题，将 `"一次编写，到处运行"` 替换为 `'一次编写，到处运行'`

3. **WorkingMemoryExample.kt**：
   - 修复了变量重新赋值问题，将 `val memory` 重命名为 `val enhancedMemory`
   - 更新了所有使用该变量的地方

4. **MemoryCompressionExample.kt**：
   - 修复了变量重新赋值问题，将 `val memory` 重命名为 `val enhancedMemory`
   - 更新了所有使用该变量的地方

## 4. 待修复的问题

### 4.1 类重复声明问题

需要修复以下文件中的类重复声明问题：

1. **AdvancedZodToolExample.kt**：
   - 重命名 `UserSearchResult` 类为 `AdvancedUserSearchResult`

2. **DataClassZodToolExample.kt**：
   - 重命名 `User` 类为 `ZodUser`

3. **ZodAdvancedToolExample.kt**：
   - 重命名 `User` 类为 `ZodAdvancedUser`
   - 重命名 `UserSearchResult` 类为 `ZodAdvancedUserSearchResult`

4. **ZodCalculatorExample.kt** 和 **ZodCalculatorToolExample.kt**：
   - 分别重命名 `CalculatorInput` 和 `CalculatorOutput` 类

### 4.2 类型不匹配问题

需要修复以下文件中的类型不匹配问题：

1. **AdvancedZodToolExample.kt**：
   - 修复 `Pattern` 类型不匹配问题
   - 修复 `Boolean` 类型不匹配问题

2. **ZodAdvancedToolExample.kt**：
   - 修复 `optional()` 和 `default()` 函数调用问题
   - 修复 Schema 类型不匹配问题

### 4.3 未解析引用问题

需要实现或导入以下缺失的引用：

1. **DataSourceExample.kt**：
   - 实现 `datasource` 包
   - 实现 `DataSourceManager` 类
   - 实现 `localFileSystem`、`restApi`、`mysql` 等数据源

2. **WorkflowRetryExample.kt**：
   - 修复 `openai` 引用问题
   - 实现 `agentStep` 相关功能

### 4.4 API 使用错误

需要修复以下文件中的 API 使用错误：

1. **EventCallbackWorkflowExample.kt**、**SuspendableWorkflowExample.kt** 和 **WorkflowEventVisualizerExample.kt**：
   - 修复 `openAi` 函数调用问题
   - 修复 `agent` 函数参数问题

2. **ZodAgentExample.kt**：
   - 修复 `MemoryFactory` 引用问题
   - 修复 `optional()` 函数调用问题

### 4.5 main 函数冲突

需要解决以下文件中的 main 函数冲突：

1. **agent/** 目录下的多个示例文件：
   - 重命名 main 函数或使用不同的包名

## 5. 修复计划

### 5.1 第一阶段：修复核心示例

1. 继续修复内存相关示例中的剩余问题
2. 修复 DeepSeek 相关示例
3. 修复简单的工具示例

### 5.2 第二阶段：修复 Zod 相关示例

1. 重构重复的数据类
2. 修复类型不匹配问题
3. 更新 API 使用

### 5.3 第三阶段：修复复杂示例

1. 实现缺失的数据源功能
2. 修复工作流相关示例
3. 解决 main 函数冲突

### 5.4 第四阶段：全面测试

1. 运行所有修复后的示例
2. 验证功能正确性
3. 完善文档和注释

## 6. 执行计划

1. 继续使用 sourceSets 配置排除有问题的文件，确保项目可以编译
2. 逐个修复文件，按照上述阶段进行
3. 每修复一个文件，就从 exclude 列表中移除，并添加到 include 列表中
4. 定期运行编译测试，确保修复进展顺利
5. 完成所有修复后，移除 sourceSets 配置中的 include 和 exclude 限制

## 7. 总结

Kastrax Examples 模块存在多种编译问题，主要包括类重复声明、类型不匹配、未解析引用、API 使用错误等。我们已经完成了部分修复工作，包括修改 build.gradle.kts 和修复内存相关示例。接下来，我们将按照分阶段计划继续修复剩余问题，最终确保所有示例都能正确编译和运行。
