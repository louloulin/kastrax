# kastrax-codebase 模块修复计划

## 问题分析

根据 Gradle 构建日志，我们发现 kastrax-codebase 模块存在以下主要问题：

### 1. 主代码编译问题
- ✅ DesignPatternDetector.kt 中的 model 引用问题
- ✅ SymbolRelationGraphBuilder.kt 和 SymbolRelationGraph.kt 中的 VARIABLE、NAMESPACE、MODULE 常量引用问题
- ✅ FlowGraph.kt 中的方法签名冲突问题
- ✅ HybridRetriever.kt 中的 combinedScore、vectorScore 和 keywordScore 引用问题

### 2. 测试代码编译问题
- ✅ ContextBuilderTest.kt 中的协程调用问题：
  ```
  Suspension functions can only be called within coroutine body
  ```
- ✅ ChapiCodeParserTest.kt 中的未解析引用问题：
  ```
  Unresolved reference 'parseCode', 'type', 'name', 'children', 'it', 'size', 'qualifiedName', 'visibility', 'metadata'
  ```
- ✅ 类型不匹配问题：Float 类型被用于需要 Double 类型的地方

## 修复计划

### 1. 修复 ContextBuilderTest.kt 中的协程调用问题

测试方法中调用了 suspend 函数，但没有在协程上下文中执行。需要使用 runBlocking 或其他协程构建器包装测试方法。

```kotlin
@Test
fun `should build context with related elements`() = runBlocking {
    // 测试代码
}
```

### 2. 修复 ChapiCodeParserTest.kt 中的未解析引用问题

ChapiCodeParserTest.kt 中使用了许多未定义的属性和方法。需要检查 ChapiCodeParser 类的实现，确保测试中使用的属性和方法与实现一致。

主要问题包括：
- parseCode 方法未定义或不可见
- 访问了不存在的属性：type, name, children, qualifiedName, visibility, metadata 等

解决方案：
1. 检查 ChapiCodeParser 类的实现
2. 更新测试代码以匹配实际实现
3. 如果需要，添加缺失的方法和属性

### 3. 修复类型不匹配问题

将 Float 类型的值改为 Double 类型：

```kotlin
// 修改前
val searchResults = listOf(
    CodeSearchResult(classElement, 0.9f),
    CodeSearchResult(methodElement, 0.8f)
)

// 修改后
val searchResults = listOf(
    CodeSearchResult(classElement, 0.9),
    CodeSearchResult(methodElement, 0.8)
)
```

同样，将 minScore 参数从 Float 改为 Double：

```kotlin
// 修改前
minScore = 0.5f

// 修改后
minScore = 0.5
```

## 实施步骤

1. 修复 ContextBuilderTest.kt 中的协程调用问题
   - 添加 runBlocking 包装测试方法
   - 导入 kotlinx.coroutines.runBlocking

2. 修复 ChapiCodeParserTest.kt 中的未解析引用问题
   - 检查 ChapiCodeParser 类的实现
   - 更新测试代码以匹配实际实现
   - 如果需要，添加缺失的方法和属性

3. 修复类型不匹配问题
   - 将 Float 类型的值改为 Double 类型
   - 将 minScore 参数从 Float 改为 Double

4. 运行测试验证修复结果
   - 执行 `./gradlew :kastrax-codebase:test`
   - 确保所有测试通过

## 验证方法

每完成一个修复步骤，都应该运行以下命令验证修复结果：

```bash
./gradlew :kastrax-codebase:compileTestKotlin --stacktrace
```

全部修复完成后，运行以下命令验证测试是否通过：

```bash
./gradlew :kastrax-codebase:test --stacktrace
```

## 完成标准

- ✅ 所有编译错误已修复
- ✅ 所有测试通过
- ✅ 代码质量符合项目标准
- ✅ 文档已更新
