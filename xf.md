# kastrax-codebase 修复计划

## 问题概述

根据编译错误日志，kastrax-codebase 模块存在以下主要问题：

1. **类型定义问题**：
   - `RetrievalResult` 类在多个地方有不同的定义，导致引用冲突
   - `HybridRetrievalResult` 和 `RetrievalSource` 类引用不存在
   - `SymbolNode` 和 `SymbolRelation` 类的构造函数参数不匹配

2. **未解析引用问题**：
   - `EmbeddingService` 和 `SemanticMemorySearchResult` 引用不存在
   - ChapiCodeParser 中的 Chapi 库引用问题（如 `Position`, `DocString` 等）
   - DesignPatternDetector 中的 `values()`, `element`, `metadata` 等引用问题
   - SymbolRelationGraph 和 SymbolQueryEngine 中的方法调用问题

3. **类型不匹配问题**：
   - 方法返回类型与期望类型不匹配
   - 参数类型与期望类型不匹配
   - 无法推断泛型类型参数

## 修复策略

采用分阶段、系统性的修复策略：

1. **统一基础类型定义**
2. **修复依赖关系**
3. **修复具体实现类**
4. **修复测试代码**

## 详细修复计划

### 阶段一：统一基础类型定义

1. **统一 RetrievalResult 类定义**
   - 在 `ai.kastrax.codebase.retrieval.model` 包中创建统一的 `RetrievalResult` 类
   - 确保所有使用 `RetrievalResult` 的地方都引用这个统一的类

2. **创建 HybridRetrievalResult 和 RetrievalSource 类**
   - 在 `ai.kastrax.codebase.retrieval.model` 包中创建这些类
   - 添加 `toRetrievalResult()` 方法以便于类型转换

3. **统一 SymbolNode 和 SymbolRelation 类定义**
   - 确保 `SymbolNode` 类有一致的构造函数参数
   - 添加辅助构造函数以支持不同的参数组合

### 阶段二：修复依赖关系

1. **修复 EmbeddingService 和 SemanticMemory 引用**
   - 确保这些类在正确的包中定义
   - 更新所有引用这些类的地方

2. **修复 Chapi 库依赖**
   - 确保 Chapi 库正确引入
   - 使用反射或适配器模式处理 Chapi 库的类型

3. **修复 SymbolType 和 SymbolRelationType 枚举**
   - 确保这些枚举类型完整定义
   - 修复所有引用这些枚举的地方

### 阶段三：修复具体实现类

1. **修复 HybridRetriever 类**
   - 修复 `retrieve` 方法的返回类型
   - 修复 `combinedScore` 引用问题
   - 修复类型转换问题

2. **修复 ChapiCodeParser 类**
   - 使用反射安全地访问 Chapi 对象的属性
   - 添加空值处理逻辑

3. **修复 DesignPatternDetector 类**
   - 修复 `values()` 方法调用
   - 修复 `element` 和 `metadata` 引用

4. **修复 SymbolRelationGraph 和 SymbolQueryEngine 类**
   - 实现缺失的方法
   - 修复方法调用问题

### 阶段四：修复测试代码

1. **更新测试用例**
   - 确保测试用例使用正确的类型
   - 修复测试数据

2. **添加新的测试**
   - 为修复的功能添加测试
   - 确保测试覆盖边界情况

## 具体修复步骤

### 1. 统一 RetrievalResult 类定义

```kotlin
// 在 ai.kastrax.codebase.retrieval.model.RetrievalResult.kt 中
package ai.kastrax.codebase.retrieval.model

import ai.kastrax.codebase.semantic.model.CodeElement

data class RetrievalResult(
    val element: CodeElement,
    val score: Double,
    val features: List<RetrievalFeature> = emptyList(),
    val explanation: String? = null
)

data class RetrievalFeature(
    val name: String,
    val weight: Double,
    val value: Double
)

data class HybridRetrievalResult(
    val element: CodeElement,
    val vectorScore: Float,
    val keywordScore: Float,
    val combinedScore: Float,
    val source: RetrievalSource
) {
    fun toRetrievalResult(): RetrievalResult {
        return RetrievalResult(
            element = element,
            score = combinedScore.toDouble(),
            explanation = "Vector score: $vectorScore, Keyword score: $keywordScore"
        )
    }
}

enum class RetrievalSource {
    VECTOR,
    KEYWORD,
    HYBRID
}
```

### 2. 修复 HybridRetriever 类

```kotlin
// 在 HybridRetriever.kt 中
import ai.kastrax.codebase.retrieval.model.HybridRetrievalResult
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.retrieval.model.RetrievalSource

// 修改 retrieve 方法
suspend fun retrieve(
    query: String,
    limit: Int = config.defaultLimit,
    minScore: Float = config.defaultMinScore
): List<RetrievalResult> = withContext(Dispatchers.Default) {
    // 先获取 HybridRetrievalResult
    val hybridResults = retrieveHybrid(query, limit, minScore)
    
    // 转换为通用的 RetrievalResult 类型
    return@withContext hybridResults.map { hybrid -> hybrid.toRetrievalResult() }
}
```

### 3. 修复 SymbolNode 类

```kotlin
// 在 SymbolNode.kt 中
data class SymbolNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val qualifiedName: String,
    val type: SymbolType,
    val kind: SymbolKind,
    val location: Location,
    val visibility: Visibility = Visibility.UNKNOWN,
    val codeElement: CodeElement? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    // 添加辅助构造函数
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        qualifiedName: String,
        type: SymbolType,
        kind: String,
        location: Location,
        visibility: Visibility = Visibility.UNKNOWN,
        codeElement: CodeElement? = null,
        metadata: MutableMap<String, Any> = mutableMapOf()
    ) : this(
        id = id,
        name = name,
        qualifiedName = qualifiedName,
        type = type,
        kind = try { SymbolKind.valueOf(kind.uppercase()) } catch (e: Exception) { SymbolKind.UNKNOWN },
        location = location,
        visibility = visibility,
        codeElement = codeElement,
        metadata = metadata
    )
}
```

### 4. 修复 ChapiCodeParser 类

```kotlin
// 在 ChapiCodeParser.kt 中
// 添加安全访问 Chapi 对象属性的方法
private fun getPosition(obj: Any): CodePosition? {
    return try {
        val positionField = obj::class.java.getDeclaredField("Position")
        positionField.isAccessible = true
        positionField.get(obj) as? CodePosition
    } catch (e: Exception) {
        null
    }
}

private fun getDocString(obj: Any): String {
    return try {
        val docStringField = obj::class.java.getDeclaredField("DocString")
        docStringField.isAccessible = true
        (docStringField.get(obj) as? String) ?: ""
    } catch (e: Exception) {
        ""
    }
}
```

### 5. 修复 SymbolRelationGraph 类

```kotlin
// 在 SymbolRelationGraph.kt 中
// 实现 convertToSymbolType 方法
private fun convertToSymbolType(codeElementType: CodeElementType): SymbolType {
    return when (codeElementType) {
        CodeElementType.FILE -> SymbolType.FILE
        CodeElementType.PACKAGE -> SymbolType.PACKAGE
        CodeElementType.CLASS -> SymbolType.CLASS
        CodeElementType.INTERFACE -> SymbolType.INTERFACE
        CodeElementType.ENUM -> SymbolType.ENUM
        CodeElementType.ANNOTATION -> SymbolType.ANNOTATION
        CodeElementType.METHOD -> SymbolType.METHOD
        CodeElementType.CONSTRUCTOR -> SymbolType.CONSTRUCTOR
        CodeElementType.FIELD -> SymbolType.FIELD
        CodeElementType.PROPERTY -> SymbolType.PROPERTY
        CodeElementType.PARAMETER -> SymbolType.PARAMETER
        CodeElementType.FUNCTION -> SymbolType.FUNCTION
        CodeElementType.VARIABLE -> SymbolType.LOCAL_VARIABLE
        CodeElementType.LOCAL_VARIABLE -> SymbolType.LOCAL_VARIABLE
        CodeElementType.IMPORT -> SymbolType.IMPORT
        CodeElementType.NAMESPACE -> SymbolType.NAMESPACE
        CodeElementType.MODULE -> SymbolType.MODULE
        CodeElementType.LAMBDA -> SymbolType.LAMBDA
        CodeElementType.BLOCK -> SymbolType.BLOCK
        CodeElementType.STATEMENT -> SymbolType.STATEMENT
        CodeElementType.EXPRESSION -> SymbolType.EXPRESSION
        CodeElementType.COMMENT -> SymbolType.COMMENT
        CodeElementType.UNKNOWN -> SymbolType.UNKNOWN
        else -> SymbolType.UNKNOWN
    }
}
```

## 执行计划

1. 首先修复基础类型定义（RetrievalResult, HybridRetrievalResult, SymbolNode 等）
2. 然后修复依赖关系（EmbeddingService, Chapi 库等）
3. 接着修复具体实现类（HybridRetriever, ChapiCodeParser 等）
4. 最后修复测试代码

每个步骤完成后，运行 Gradle 构建检查是否还有其他错误，并根据需要调整修复计划。

## 注意事项

1. 修复过程中保持谨慎，避免引入新的问题
2. 优先修复基础类型和依赖关系，然后再修复具体实现
3. 使用反射和适配器模式处理外部库依赖问题
4. 添加适当的注释说明修复内容
5. 修复完成后进行全面测试，确保功能正常
