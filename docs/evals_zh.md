# KastraX 评估框架

## 1. 概述

KastraX 评估框架（kastrax-evals）是一个用于评估 AI 系统输出质量的工具。它提供了一套灵活的评估器和报告生成工具，可以帮助开发者评估和改进 AI 系统的性能。

评估框架的主要功能包括：

- 多种内置评估器，用于评估文本输出的质量
- 支持自定义评估器，以满足特定需求
- 并行运行多个评估器，提高效率
- 生成多种格式的评估报告（Markdown、JSON、HTML、CSV）
- 支持使用 AI 代理进行评估

## 2. 核心组件

### 2.1 评估器（Evaluator）

评估器是评估框架的核心组件，用于评估 AI 系统的输出。每个评估器都实现了 `Evaluator` 接口：

```kotlin
interface Evaluator<I, O> {
    val name: String
    val description: String
    
    suspend fun evaluate(
        input: I,
        output: O,
        options: Map<String, Any?> = emptyMap()
    ): EvaluationResult
}
```

其中：
- `I` 是输入类型
- `O` 是输出类型
- `name` 是评估器的名称
- `description` 是评估器的描述
- `evaluate` 方法用于评估输出，返回评估结果

### 2.2 评估结果（EvaluationResult）

评估结果包含评估的分数和详细信息：

```kotlin
data class EvaluationResult(
    val score: Double,
    val details: Map<String, Any?> = emptyMap()
)
```

其中：
- `score` 是评估分数，通常在 0.0 到 1.0 之间
- `details` 是评估的详细信息，可以包含评估过程中的中间结果、解释等

### 2.3 评估运行器（EvaluationRunner）

评估运行器用于运行一组评估器：

```kotlin
class EvaluationRunner<I, O>(
    private val evaluators: List<Evaluator<I, O>>
)
```

它提供了以下方法：
- `runAll`：运行所有评估器
- `run`：运行单个评估器

### 2.4 评估报告（EvaluationReport）

评估报告包含多个评估运行的结果：

```kotlin
data class EvaluationReport<I, O>(
    val results: List<EvaluationRunResult<I, O>>
)
```

它提供了以下方法：
- `averageScore`：获取平均分数
- `totalDurationMs`：获取总持续时间
- `summary`：获取评估报告的摘要
- `toJson`：将评估报告转换为 JSON 字符串

### 2.5 报告生成器（ReportGenerator）

报告生成器用于生成评估报告：

```kotlin
class ReportGenerator {
    fun <I, O> generateReport(
        report: EvaluationReport<I, O>,
        format: ReportFormat = ReportFormat.MARKDOWN
    ): String
    
    fun <I, O> saveReport(
        report: EvaluationReport<I, O>,
        filePath: String,
        format: ReportFormat = ReportFormat.MARKDOWN
    )
}
```

支持的报告格式包括：
- `JSON`：JSON 格式
- `MARKDOWN`：Markdown 格式
- `HTML`：HTML 格式
- `CSV`：CSV 格式

## 3. 内置评估器

### 3.1 文本评估器

#### 3.1.1 精确匹配评估器（ExactMatch）

评估输出是否与预期输出完全匹配：

```kotlin
val evaluator = exactMatchEvaluator()
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = "The capital of France is Paris.",
    options = mapOf(
        "expected" to "The capital of France is Paris.",
        "ignoreCase" to true,
        "ignoreWhitespace" to true
    )
)
```

选项：
- `expected`：预期输出，默认为输入
- `ignoreCase`：是否忽略大小写，默认为 false
- `ignoreWhitespace`：是否忽略空白，默认为 false

#### 3.1.2 包含关键词评估器（ContainsKeywords）

评估输出是否包含所有指定的关键词：

```kotlin
val evaluator = containsKeywordsEvaluator()
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = "The capital of France is Paris.",
    options = mapOf(
        "keywords" to listOf("capital", "France", "Paris"),
        "requireAll" to true,
        "ignoreCase" to true
    )
)
```

选项：
- `keywords`：关键词列表
- `requireAll`：是否要求包含所有关键词，默认为 true
- `ignoreCase`：是否忽略大小写，默认为 true

#### 3.1.3 正则表达式匹配评估器（RegexMatch）

评估输出是否匹配指定的正则表达式：

```kotlin
val evaluator = regexMatchEvaluator()
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = "The capital of France is Paris.",
    options = mapOf(
        "pattern" to "capital of (\\w+) is (\\w+)"
    )
)
```

选项：
- `pattern`：正则表达式模式

#### 3.1.4 相似度评估器（Similarity）

评估输出与预期输出的相似度：

```kotlin
val evaluator = similarityEvaluator()
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = "The capital of France is Paris.",
    options = mapOf(
        "expected" to "The capital of France is Paris.",
        "method" to "jaccard"
    )
)
```

选项：
- `expected`：预期输出，默认为输入
- `method`：相似度计算方法，可选值为 "jaccard" 或 "levenshtein"，默认为 "jaccard"

### 3.2 AI 评估器

#### 3.2.1 AI 评估器（AIEvaluator）

使用 AI 代理评估输出的质量：

```kotlin
val evaluator = aiEvaluator(agent)
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = "The capital of France is Paris.",
    options = mapOf(
        "criteria" to "质量、相关性和准确性",
        "scoreOnly" to false
    )
)
```

选项：
- `criteria`：评估标准，默认为 "质量、相关性和准确性"
- `scoreOnly`：是否只返回分数，默认为 false
- `rubric`：评分标准，可选

#### 3.2.2 AI 对比评估器（AIComparisonEvaluator）

使用 AI 代理比较两个输出的质量：

```kotlin
val evaluator = aiComparisonEvaluator(agent)
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = Pair("The capital of France is Paris.", "Paris is the capital of France."),
    options = mapOf(
        "criteria" to "质量、相关性和准确性",
        "preferenceOnly" to false
    )
)
```

选项：
- `criteria`：评估标准，默认为 "质量、相关性和准确性"
- `preferenceOnly`：是否只返回偏好，默认为 false

#### 3.2.3 AI 分类评估器（AIClassificationEvaluator）

使用 AI 代理对输出进行分类：

```kotlin
val evaluator = aiClassificationEvaluator(agent)
val result = evaluator.evaluate(
    input = "What is the capital of France?",
    output = "The capital of France is Paris.",
    options = mapOf(
        "categories" to listOf("地理", "历史", "科学", "艺术"),
        "labelOnly" to false
    )
)
```

选项：
- `categories`：分类类别列表
- `labelOnly`：是否只返回类别标签，默认为 false

## 4. 自定义评估器

你可以使用 DSL 创建自定义评估器：

```kotlin
val customEvaluator = evaluator<String, String> {
    name = "CustomEvaluator"
    description = "自定义评估器"
    
    evaluate { input, output, options ->
        // 实现自定义评估逻辑
        val score = calculateScore(input, output, options)
        val details = mapOf("customDetail" to "自定义详情")
        
        EvaluationResult(score, details)
    }
}
```

## 5. 使用示例

### 5.1 基本使用

```kotlin
// 创建评估器
val exactMatchEval = exactMatchEvaluator()
val containsKeywordsEval = containsKeywordsEvaluator()

// 创建评估运行器
val runner = EvaluationRunner(listOf(exactMatchEval, containsKeywordsEval))

// 准备输入和输出
val input = "What is the capital of France?"
val output = "The capital of France is Paris."

// 准备评估选项
val exactMatchOptions = mapOf(
    "expected" to "The capital of France is Paris.",
    "ignoreCase" to true
)

val keywordsOptions = mapOf(
    "keywords" to listOf("capital", "France", "Paris")
)

// 运行评估
val results = runner.runAll(input, output, mapOf(
    "ExactMatch" to exactMatchOptions,
    "ContainsKeywords" to keywordsOptions
))

// 创建评估报告
val report = EvaluationReport(results)

// 生成报告
val reportGenerator = ReportGenerator()
val markdownReport = reportGenerator.generateReport(report, ReportFormat.MARKDOWN)

// 保存报告
reportGenerator.saveReport(report, "evaluation_report.md", ReportFormat.MARKDOWN)
```

### 5.2 使用 AI 评估器

```kotlin
// 创建 AI 代理
val agent = createAgent()

// 创建 AI 评估器
val aiEval = aiEvaluator(agent)

// 创建评估运行器
val runner = EvaluationRunner(listOf(aiEval))

// 准备输入和输出
val input = "What is the capital of France?"
val output = "The capital of France is Paris."

// 准备评估选项
val aiOptions = mapOf(
    "criteria" to "准确性、完整性和相关性"
)

// 运行评估
val result = runner.run(aiEval, input, output, aiOptions)

// 打印结果
println("AI 评估分数: ${result.result.score}")
println("AI 评估详情: ${result.result.details}")
```

## 6. 最佳实践

### 6.1 选择合适的评估器

根据评估需求选择合适的评估器：
- 对于需要精确匹配的场景，使用 `exactMatchEvaluator`
- 对于需要检查关键词的场景，使用 `containsKeywordsEvaluator`
- 对于需要模式匹配的场景，使用 `regexMatchEvaluator`
- 对于需要计算相似度的场景，使用 `similarityEvaluator`
- 对于需要主观评估的场景，使用 `aiEvaluator`

### 6.2 组合多个评估器

通常，单个评估器无法全面评估输出的质量。建议组合多个评估器，从不同角度评估输出：

```kotlin
val runner = EvaluationRunner(listOf(
    exactMatchEvaluator(),
    containsKeywordsEvaluator(),
    similarityEvaluator(),
    aiEvaluator(agent)
))
```

### 6.3 自定义评估逻辑

对于特定的评估需求，可以创建自定义评估器：

```kotlin
val domainSpecificEvaluator = evaluator<String, String> {
    name = "DomainSpecificEvaluator"
    description = "领域特定评估器"
    
    evaluate { input, output, options ->
        // 实现领域特定的评估逻辑
        val score = domainSpecificEvaluation(input, output)
        EvaluationResult(score)
    }
}
```

### 6.4 生成详细报告

使用报告生成器生成详细的评估报告，以便分析和改进：

```kotlin
val reportGenerator = ReportGenerator()

// 生成 Markdown 报告
reportGenerator.saveReport(report, "evaluation_report.md", ReportFormat.MARKDOWN)

// 生成 HTML 报告
reportGenerator.saveReport(report, "evaluation_report.html", ReportFormat.HTML)
```

## 7. 总结

KastraX 评估框架提供了一套灵活、可扩展的工具，用于评估 AI 系统的输出质量。通过使用内置评估器、自定义评估器和报告生成工具，开发者可以全面评估和改进 AI 系统的性能。
