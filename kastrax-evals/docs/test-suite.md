# 测试套件 (Test Suite)

## 概述

测试套件是评估框架的重要组成部分，用于组织和管理测试用例，批量评估 AI 系统的性能，并进行基准测试和比较。KastraX 评估框架提供了一套完整的测试套件功能，包括测试用例管理、批量评估和基准测试。

## 测试用例管理

### 测试用例 (TestCase)

测试用例是评估 AI 系统的基本单元，包含输入、期望输出、评估指标和评估选项等信息。

```kotlin
// 创建测试用例
val testCase = testCase<String, String> {
    name = "Capital of France"
    description = "Test if the system knows the capital of France"
    tag("geography")
    tag("europe")
    input = "What is the capital of France?"
    expectedOutput = "Paris"
    metric(accuracyMetric)
    metric(relevanceMetric)
    option("caseSensitive", false)
    threshold = 0.7
}

// 执行测试用例
val result = testCase.execute("Paris")
println("Test passed: ${result.passed}")
```

### 测试套件 (TestSuite)

测试套件用于组织和管理测试用例，可以按标签分组和筛选测试用例。

```kotlin
// 创建测试套件
val testSuite = testSuite<String, String> {
    name = "Geography Test Suite"
    description = "A test suite for geography questions"
    tag("geography")
    
    // 添加测试用例
    testCase {
        name = "Capital of France"
        input = "What is the capital of France?"
        expectedOutput = "Paris"
        metric(accuracyMetric)
        tag("europe")
    }
    
    testCase {
        name = "Capital of Japan"
        input = "What is the capital of Japan?"
        expectedOutput = "Tokyo"
        metric(accuracyMetric)
        tag("asia")
    }
}

// 按标签获取测试用例
val europeTestCases = testSuite.getTestCasesByTag("europe")
val asiaTestCases = testSuite.getTestCasesByTag("asia")
```

### 测试用例生成工具 (TestCaseGenerator)

测试用例生成工具可以自动生成测试用例，支持从 JSON 文件、CSV 文件加载测试用例，或使用 LLM 生成测试用例。

```kotlin
// 从 JSON 文件加载测试用例
val jsonTestCases = loadTestCasesFromJson(
    file = File("test_cases.json"),
    metrics = listOf(accuracyMetric),
    inputConverter = { it },
    outputConverter = { it }
)

// 从 CSV 文件加载测试用例
val csvTestCases = loadTestCasesFromCsv(
    file = File("test_cases.csv"),
    metrics = listOf(accuracyMetric),
    inputConverter = { it },
    outputConverter = { it },
    delimiter = ",",
    hasHeader = true
)

// 使用 LLM 生成测试用例
val llmGenerator = LlmTestCaseGenerator(
    llmClient = llmClient,
    inputConverter = { it },
    outputConverter = { it }
)

val llmTestCases = llmGenerator.generate(
    count = 10,
    metrics = listOf(accuracyMetric),
    options = mapOf(
        "domain" to "geography",
        "complexity" to "medium"
    )
)
```

## 批量评估

### 输出提供者 (OutputProvider)

输出提供者用于获取 AI 系统的输出，是批量评估的关键组件。

```kotlin
// 创建输出提供者
val outputProvider = object : OutputProvider<String, String> {
    override suspend fun getOutput(input: String): String {
        // 调用 AI 系统获取输出
        return aiSystem.generateResponse(input)
    }
}
```

### 批量评估器 (BatchEvaluator)

批量评估器用于批量评估 AI 系统，支持并行执行测试套件。

```kotlin
// 创建批量评估器
val batchEvaluator = BasicBatchEvaluator<String, String>()

// 批量评估
val result = batchEvaluator.evaluate(
    testSuites = listOf(geographyTestSuite, mathTestSuite),
    outputProvider = outputProvider,
    parallel = true
)

// 输出结果
println("Passed: ${result.passedCount}")
println("Failed: ${result.failedCount}")
println("Pass rate: ${result.passRate}")
```

### 结果聚合器 (ResultAggregator)

结果聚合器用于聚合评估结果，提供各种统计信息。

```kotlin
// 创建结果聚合器
val aggregator = BasicResultAggregator<String, String>()

// 聚合测试用例结果
val testCaseStats = aggregator.aggregateTestCaseResults(result.testSuiteResults.flatMap { it.testCaseResults })
println("Average score: ${testCaseStats["averageScore"]}")

// 聚合测试套件结果
val testSuiteStats = aggregator.aggregateTestSuiteResults(result.testSuiteResults)
println("Average pass rate: ${testSuiteStats["averagePassRate"]}")

// 聚合批量评估结果
val batchStats = aggregator.aggregateBatchResult(result)
println("Total test cases: ${batchStats["totalTestCases"]}")
```

## 基准测试

### 基准测试 (Benchmark)

基准测试用于比较不同 AI 系统的性能，支持多个测试套件。

```kotlin
// 创建基准测试
val benchmark = benchmark<String, String> {
    name = "General Knowledge Benchmark"
    description = "A benchmark for general knowledge questions"
    testSuite(geographyTestSuite)
    testSuite(mathTestSuite)
}

// 运行基准测试
val result = benchmark.run(
    outputProviders = mapOf(
        "System A" to systemAProvider,
        "System B" to systemBProvider,
        "System C" to systemCProvider
    ),
    parallel = true
)

// 输出结果
for ((systemName, systemResult) in result.systemResults) {
    println("$systemName: ${systemResult.passRate}")
}
```

### 常见 AI 任务的基准测试

KastraX 评估框架提供了多种常见 AI 任务的基准测试，包括问答、摘要、翻译等。

```kotlin
// 创建问答基准测试
val qaBenchmark = createQABenchmark(
    name = "问答基准测试",
    description = "评估 AI 系统回答问题的能力",
    testCases = qaTestCases,
    llmClient = llmClient
)

// 创建摘要基准测试
val summarizationBenchmark = createSummarizationBenchmark(
    name = "摘要基准测试",
    description = "评估 AI 系统生成摘要的能力",
    testCases = summarizationTestCases,
    llmClient = llmClient
)

// 创建翻译基准测试
val translationBenchmark = createTranslationBenchmark(
    name = "翻译基准测试",
    description = "评估 AI 系统翻译文本的能力",
    testCases = translationTestCases,
    llmClient = llmClient
)
```

### 性能基准测试

性能基准测试用于评估 AI 系统的性能，如响应时间、吞吐量等。

```kotlin
// 创建性能基准测试
val performanceBenchmark = createPerformanceBenchmark(
    name = "性能基准测试",
    description = "评估 AI 系统的性能",
    inputGenerator = { "What is the capital of ${countries.random()}?" },
    inputCount = 100,
    targetTimeMs = 1000,
    maxTimeMs = 5000
)
```

### 比较基准工具 (BenchmarkComparator)

比较基准工具用于比较不同 AI 系统的性能，提供各种比较指标。

```kotlin
// 创建比较器
val comparator = BenchmarkComparator<String, String>()

// 比较结果
val comparisonResult = comparator.compare(benchmarkResult)

// 输出比较结果
println("Best system: ${comparisonResult.bestSystem}")
println("Worst system: ${comparisonResult.worstSystem}")

for (comparison in comparisonResult.systemComparisons) {
    println("${comparison.system1} vs ${comparison.system2}: ${comparison.passRateDifference}")
}
```

## 并行评估支持

KastraX 评估框架支持并行执行测试用例和测试套件，可以显著提高评估效率。

```kotlin
// 串行执行
val serialResult = testSuite.execute(outputProvider, parallel = false)

// 并行执行
val parallelResult = testSuite.execute(outputProvider, parallel = true)

// 并行批量评估
val parallelBatchResult = batchEvaluator.evaluate(
    testSuites = testSuites,
    outputProvider = outputProvider,
    parallel = true
)

// 并行基准测试
val parallelBenchmarkResult = benchmark.run(
    outputProviders = outputProviders,
    parallel = true
)
```

## 结论

KastraX 评估框架提供了一套完整的测试套件功能，包括测试用例管理、批量评估和基准测试。通过使用这些功能，可以全面评估 AI 系统的性能，发现问题并进行改进。

测试套件是评估框架的核心组件，为评估报告和可视化提供了必要的支持。在后续的开发中，我们将继续完善测试套件功能，并实现更多的功能，如评估报告、可视化工具和持续评估等。
