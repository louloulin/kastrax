# 评估指标 (Evaluation Metrics)

## 概述

评估指标是评估框架的核心组件，用于衡量 AI 系统的性能和质量。KastraX 评估框架提供了一套丰富的评估指标，包括基础指标、LLM 辅助评估指标和自定义指标，可以全面评估 AI 系统的各个方面。

## 基础指标

### 准确性指标 (AccuracyMetric)

准确性指标用于评估 AI 系统输出的准确性，支持多种评估方法：

- **精确匹配 (EXACT_MATCH)**：输出必须与期望输出完全匹配
- **部分匹配 (PARTIAL_MATCH)**：输出包含期望输出的一部分
- **相似度匹配 (SIMILARITY)**：输出与期望输出的相似度达到一定阈值

```kotlin
// 创建准确性指标
val accuracyMetric = accuracyMetric(
    method = AccuracyMethod.EXACT_MATCH,
    ignoreCase = true,
    ignoreWhitespace = true
)

// 使用准确性指标评估输出
val result = accuracyMetric.calculate(
    input = "What is the capital of France?",
    output = "Paris",
    options = mapOf("expected" to "Paris")
)

println("Accuracy score: ${result.score}")
```

### 相关性指标 (RelevanceMetric)

相关性指标用于评估 AI 系统输出与输入的相关程度，支持多种评估方法：

- **关键词匹配 (KEYWORD_MATCH)**：检查输出是否包含输入中的关键词
- **语义相似度 (SEMANTIC_SIMILARITY)**：计算输出与输入的语义相似度
- **混合评估 (HYBRID)**：结合关键词匹配和语义相似度

```kotlin
// 创建相关性指标
val relevanceMetric = relevanceMetric(
    method = RelevanceMethod.HYBRID,
    embeddingService = embeddingService,
    keywordWeight = 0.7,
    semanticWeight = 0.3
)

// 使用相关性指标评估输出
val result = relevanceMetric.calculate(
    input = "What is artificial intelligence?",
    output = "Artificial intelligence is a branch of computer science that aims to create systems capable of performing tasks that normally require human intelligence."
)

println("Relevance score: ${result.score}")
```

### 响应时间指标 (ResponseTimeMetric)

响应时间指标用于评估 AI 系统的响应时间，支持多种评估方法：

- **阈值评估 (THRESHOLD)**：根据响应时间是否超过阈值进行评估
- **线性评估 (LINEAR)**：根据响应时间与目标时间的线性关系进行评估
- **指数评估 (EXPONENTIAL)**：根据响应时间与目标时间的指数关系进行评估

```kotlin
// 创建响应时间指标
val responseTimeMetric = responseTimeMetric(
    method = ResponseTimeMethod.LINEAR,
    targetTimeMs = 1000,
    maxTimeMs = 5000
)

// 使用响应时间指标评估输出
val result = responseTimeMetric.calculate(
    input = "input",
    output = "output",
    options = mapOf("responseTimeMs" to 2000L)
)

println("Response time score: ${result.score}")
```

## LLM 辅助评估指标

### 回答正确性评估 (CorrectnessMetric)

回答正确性评估指标使用 LLM 评估回答的正确性，可以根据参考答案或 LLM 的知识进行评估。

```kotlin
// 创建回答正确性评估指标
val correctnessMetric = correctnessMetric(llmClient)

// 使用回答正确性评估指标评估输出
val result = correctnessMetric.calculate(
    input = "What is the capital of France?",
    output = "Paris",
    options = mapOf("reference" to "The capital of France is Paris.")
)

println("Correctness score: ${result.score}")
```

### 回答有用性评估 (UsefulnessMetric)

回答有用性评估指标使用 LLM 评估回答对用户的有用性，考虑回答是否解决了用户的问题，是否提供了足够的信息，是否清晰易懂等因素。

```kotlin
// 创建回答有用性评估指标
val usefulnessMetric = usefulnessMetric(llmClient)

// 使用回答有用性评估指标评估输出
val result = usefulnessMetric.calculate(
    input = "How can I improve my English speaking skills?",
    output = "To improve your English speaking skills, you can practice speaking every day, talk to native speakers, watch English movies and TV shows, join language exchange groups, use language learning apps, read English articles aloud, record yourself speaking English and listen to it, don't be afraid to make mistakes, and be patient and persistent."
)

println("Usefulness score: ${result.score}")
```

## 自定义指标

### 指标接口 (Metric)

KastraX 评估框架提供了一个灵活的指标接口，可以轻松创建自定义指标：

```kotlin
// 创建自定义指标
val customMetric = metric<String, String> {
    name = "CustomMetric"
    description = "A custom metric"
    category = MetricCategory.CUSTOM
    calculate { input, output, options ->
        // 自定义评估逻辑
        val score = /* ... */
        MetricResult(score)
    }
}
```

### 指标注册 (MetricRegistry)

指标注册机制允许注册和管理指标，方便在不同地方使用：

```kotlin
// 注册指标
MetricRegistry.register(accuracyMetric)
MetricRegistry.register(relevanceMetric)
MetricRegistry.register(customMetric)

// 获取指标
val metric = MetricRegistry.get<String, String>("AccuracyMetric")

// 获取所有指标
val allMetrics = MetricRegistry.getAll()

// 获取指定类别的指标
val accuracyMetrics = MetricRegistry.getByCategory(MetricCategory.ACCURACY)
```

### 指标组合 (CompositeMetric)

指标组合功能允许将多个指标组合成一个指标，可以设置不同的权重：

```kotlin
// 创建组合指标
val compositeMetric = compositeMetric(
    name = "CompositeMetric",
    description = "A composite metric",
    category = MetricCategory.CUSTOM,
    accuracyMetric to 0.5,
    relevanceMetric to 0.3,
    responseTimeMetric to 0.2
)

// 使用组合指标评估输出
val result = compositeMetric.calculate(
    input = "What is the capital of France?",
    output = "Paris",
    options = mapOf(
        "expected" to "Paris",
        "responseTimeMs" to 1000L
    )
)

println("Composite score: ${result.score}")
```

## 指标运行器 (MetricRunner)

指标运行器用于运行指标，提供了运行单个指标和多个指标的方法：

```kotlin
// 创建指标运行器
val runner = MetricRunner<String, String>()

// 运行单个指标
val result = runner.run(
    metric = accuracyMetric,
    input = "What is the capital of France?",
    output = "Paris",
    options = mapOf("expected" to "Paris")
)

// 运行多个指标
val results = runner.runAll(
    metrics = listOf(accuracyMetric, relevanceMetric, responseTimeMetric),
    input = "What is the capital of France?",
    output = "Paris",
    options = mapOf(
        "expected" to "Paris",
        "responseTimeMs" to 1000L
    )
)
```

## 结论

KastraX 评估框架提供了一套丰富的评估指标，可以全面评估 AI 系统的各个方面。通过使用这些指标，可以深入了解 AI 系统的性能和质量，发现问题并进行改进。

评估指标是评估框架的基础，为测试套件和报告可视化提供了必要的支持。在后续的开发中，我们将继续完善评估指标，并实现更多的功能，如测试用例管理、批量评估、基准测试、评估报告、可视化工具和持续评估等。
