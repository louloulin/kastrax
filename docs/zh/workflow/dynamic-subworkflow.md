# 动态工作流和子工作流

本文档详细介绍了 KastraX 工作流引擎中的动态工作流和子工作流功能，包括动态工作流创建、子工作流嵌套和工作流模板。

## 1. 动态工作流概述

动态工作流是指在运行时根据条件或数据动态创建和执行的工作流。与静态工作流（在代码中预定义）不同，动态工作流可以根据实际情况动态调整其结构和行为，提供更大的灵活性。

## 2. 动态工作流创建

KastraX 工作流引擎支持在运行时动态创建工作流。

### 2.1 使用 WorkflowBuilder 创建动态工作流

```kotlin
// 创建工作流构建器
val workflowBuilder = WorkflowBuilder()
    .withName("dynamic-workflow")
    .withDescription("Dynamically created workflow")

// 添加步骤
workflowBuilder.addStep(
    StepBuilder()
        .withId("step1")
        .withName("Step 1")
        .withDescription("First step")
        .withAgent(agent1)
        .build()
)

workflowBuilder.addStep(
    StepBuilder()
        .withId("step2")
        .withName("Step 2")
        .withDescription("Second step")
        .withAgent(agent2)
        .withAfter(listOf("step1"))
        .withVariables(mapOf(
            "input" to VariableReference("$.steps.step1.output.result")
        ))
        .build()
)

// 构建工作流
val dynamicWorkflow = workflowBuilder.build()

// 执行动态工作流
workflowEngine.execute(dynamicWorkflow, input)
```

### 2.2 基于条件创建动态工作流

```kotlin
fun createWorkflowBasedOnData(data: Map<String, Any>): Workflow {
    val workflowBuilder = WorkflowBuilder()
        .withName("data-based-workflow")
        .withDescription("Workflow created based on input data")
    
    // 添加通用步骤
    workflowBuilder.addStep(
        StepBuilder()
            .withId("data_validation")
            .withName("Data Validation")
            .withDescription("Validate input data")
            .withAgent(validationAgent)
            .build()
    )
    
    // 根据数据类型添加不同的处理步骤
    when (data["type"]) {
        "text" -> {
            workflowBuilder.addStep(
                StepBuilder()
                    .withId("text_processing")
                    .withName("Text Processing")
                    .withDescription("Process text data")
                    .withAgent(textProcessingAgent)
                    .withAfter(listOf("data_validation"))
                    .build()
            )
        }
        "image" -> {
            workflowBuilder.addStep(
                StepBuilder()
                    .withId("image_processing")
                    .withName("Image Processing")
                    .withDescription("Process image data")
                    .withAgent(imageProcessingAgent)
                    .withAfter(listOf("data_validation"))
                    .build()
            )
        }
        "audio" -> {
            workflowBuilder.addStep(
                StepBuilder()
                    .withId("audio_processing")
                    .withName("Audio Processing")
                    .withDescription("Process audio data")
                    .withAgent(audioProcessingAgent)
                    .withAfter(listOf("data_validation"))
                    .build()
            )
        }
    }
    
    // 添加结果汇总步骤
    val processingStepId = when (data["type"]) {
        "text" -> "text_processing"
        "image" -> "image_processing"
        "audio" -> "audio_processing"
        else -> "data_validation"
    }
    
    workflowBuilder.addStep(
        StepBuilder()
            .withId("result_summary")
            .withName("Result Summary")
            .withDescription("Summarize processing results")
            .withAgent(summaryAgent)
            .withAfter(listOf(processingStepId))
            .build()
    )
    
    return workflowBuilder.build()
}

// 使用函数创建动态工作流
val inputData = mapOf("type" to "image", "data" to imageBytes)
val workflow = createWorkflowBasedOnData(inputData)
workflowEngine.execute(workflow, inputData)
```

## 3. 子工作流

子工作流是嵌套在父工作流中的工作流，允许将复杂的工作流分解为更小、更可管理的部分。

### 3.1 创建子工作流

```kotlin
// 定义子工作流
val dataProcessingSubworkflow = workflow {
    name = "data-processing-subworkflow"
    description = "Process data as a subworkflow"
    
    step(dataLoadingAgent) {
        id = "data_loading"
        name = "Data Loading"
        description = "Load data from source"
        variables = mapOf(
            "source" to variable("$.input.source")
        )
    }
    
    step(dataTransformationAgent) {
        id = "data_transformation"
        name = "Data Transformation"
        description = "Transform loaded data"
        after("data_loading")
        variables = mapOf(
            "data" to variable("$.steps.data_loading.output.data")
        )
    }
    
    step(dataValidationAgent) {
        id = "data_validation"
        name = "Data Validation"
        description = "Validate transformed data"
        after("data_transformation")
        variables = mapOf(
            "data" to variable("$.steps.data_transformation.output.transformedData")
        )
    }
}

// 在父工作流中使用子工作流
val parentWorkflow = workflow {
    name = "parent-workflow"
    description = "Parent workflow using subworkflow"
    
    step(inputPreparationAgent) {
        id = "input_preparation"
        name = "Input Preparation"
        description = "Prepare input for data processing"
    }
    
    // 使用子工作流步骤
    subworkflow {
        id = "data_processing"
        name = "Data Processing"
        description = "Process data using subworkflow"
        after("input_preparation")
        workflow = dataProcessingSubworkflow
        variables = mapOf(
            "source" to variable("$.steps.input_preparation.output.source")
        )
    }
    
    step(resultAnalysisAgent) {
        id = "result_analysis"
        name = "Result Analysis"
        description = "Analyze processing results"
        after("data_processing")
        variables = mapOf(
            "validatedData" to variable("$.steps.data_processing.output.steps.data_validation.output.validatedData")
        )
    }
}
```

### 3.2 子工作流输入和输出映射

子工作流可以通过输入和输出映射与父工作流交换数据：

```kotlin
// 在父工作流中使用子工作流，带输入和输出映射
subworkflow {
    id = "data_processing"
    name = "Data Processing"
    description = "Process data using subworkflow"
    after("input_preparation")
    workflow = dataProcessingSubworkflow
    
    // 输入映射
    inputMapping = mapOf(
        "source" to variable("$.steps.input_preparation.output.source"),
        "options" to variable("$.input.processingOptions")
    )
    
    // 输出映射
    outputMapping = mapOf(
        "processedData" to variable("$.steps.data_validation.output.validatedData"),
        "metadata" to variable("$.steps.data_validation.output.metadata")
    )
}

// 在后续步骤中使用子工作流的映射输出
step(resultAnalysisAgent) {
    id = "result_analysis"
    name = "Result Analysis"
    description = "Analyze processing results"
    after("data_processing")
    variables = mapOf(
        "data" to variable("$.steps.data_processing.output.processedData"),
        "metadata" to variable("$.steps.data_processing.output.metadata")
    )
}
```

### 3.3 子工作流上下文隔离

子工作流在自己的上下文中执行，与父工作流隔离：

```kotlin
// 定义带有上下文隔离的子工作流
subworkflow {
    id = "isolated_processing"
    name = "Isolated Processing"
    description = "Process data in isolated context"
    after("input_preparation")
    workflow = dataProcessingSubworkflow
    
    // 配置上下文隔离
    config = SubworkflowConfig(
        contextIsolation = ContextIsolation.FULL,  // 完全隔离
        variableScope = VariableScope.SUBWORKFLOW_ONLY  // 只能访问子工作流变量
    )
    
    // 输入映射（显式传递所有需要的变量）
    inputMapping = mapOf(
        "source" to variable("$.steps.input_preparation.output.source"),
        "options" to variable("$.input.processingOptions")
    )
}
```

### 3.4 子工作流错误处理

```kotlin
// 定义带有错误处理的子工作流
subworkflow {
    id = "data_processing"
    name = "Data Processing"
    description = "Process data using subworkflow"
    after("input_preparation")
    workflow = dataProcessingSubworkflow
    
    // 配置错误处理
    config = SubworkflowConfig(
        errorHandling = ErrorHandling.CONTINUE_PARENT_ON_ERROR,  // 子工作流失败时继续执行父工作流
        errorOutput = mapOf(  // 子工作流失败时的默认输出
            "processedData" to emptyList<Any>(),
            "status" to "failed"
        )
    )
    
    // 错误回调
    onError { error ->
        println("Subworkflow failed: ${error.message}")
        notificationService.sendAlert("Subworkflow failed", error.message ?: "Unknown error")
    }
}
```

## 4. 工作流模板

工作流模板是可重用的工作流定义，可以通过参数化来适应不同的使用场景。

### 4.1 创建工作流模板

```kotlin
// 定义工作流模板
val dataProcessingTemplate = workflowTemplate {
    name = "data-processing-template"
    description = "Template for data processing workflows"
    
    // 定义模板参数
    parameter<String>("dataSource") {
        description = "Source of the data to process"
        required = true
    }
    
    parameter<String>("outputFormat") {
        description = "Format of the output data"
        defaultValue = "json"
        allowedValues = listOf("json", "xml", "csv")
    }
    
    parameter<Int>("batchSize") {
        description = "Size of data batches to process"
        defaultValue = 100
        validation { it > 0 }
    }
    
    // 定义工作流步骤
    step(dataLoadingAgent) {
        id = "data_loading"
        name = "Data Loading"
        description = "Load data from source"
        variables = mapOf(
            "source" to parameter("dataSource"),
            "batchSize" to parameter("batchSize")
        )
    }
    
    step(dataProcessingAgent) {
        id = "data_processing"
        name = "Data Processing"
        description = "Process loaded data"
        after("data_loading")
        variables = mapOf(
            "data" to variable("$.steps.data_loading.output.data"),
            "batchSize" to parameter("batchSize")
        )
    }
    
    step(dataExportAgent) {
        id = "data_export"
        name = "Data Export"
        description = "Export processed data"
        after("data_processing")
        variables = mapOf(
            "data" to variable("$.steps.data_processing.output.processedData"),
            "format" to parameter("outputFormat")
        )
    }
}
```

### 4.2 实例化工作流模板

```kotlin
// 实例化工作流模板
val s3ProcessingWorkflow = dataProcessingTemplate.instantiate(
    parameters = mapOf(
        "dataSource" to "s3://data-bucket/input",
        "outputFormat" to "json",
        "batchSize" to 200
    )
)

// 执行实例化的工作流
workflowEngine.execute(s3ProcessingWorkflow, input)
```

### 4.3 工作流模板继承和组合

```kotlin
// 基础数据处理模板
val baseDataProcessingTemplate = workflowTemplate {
    name = "base-data-processing-template"
    description = "Base template for data processing"
    
    parameter<String>("dataSource") {
        description = "Source of the data to process"
        required = true
    }
    
    step(dataLoadingAgent) {
        id = "data_loading"
        name = "Data Loading"
        description = "Load data from source"
        variables = mapOf(
            "source" to parameter("dataSource")
        )
    }
    
    step(dataProcessingAgent) {
        id = "data_processing"
        name = "Data Processing"
        description = "Process loaded data"
        after("data_loading")
        variables = mapOf(
            "data" to variable("$.steps.data_loading.output.data")
        )
    }
}

// 扩展模板，添加数据导出功能
val extendedDataProcessingTemplate = workflowTemplate {
    name = "extended-data-processing-template"
    description = "Extended template with data export"
    
    // 继承基础模板
    extends(baseDataProcessingTemplate)
    
    // 添加新参数
    parameter<String>("outputFormat") {
        description = "Format of the output data"
        defaultValue = "json"
    }
    
    // 添加新步骤
    step(dataExportAgent) {
        id = "data_export"
        name = "Data Export"
        description = "Export processed data"
        after("data_processing")
        variables = mapOf(
            "data" to variable("$.steps.data_processing.output.processedData"),
            "format" to parameter("outputFormat")
        )
    }
}
```

## 5. 动态步骤生成

KastraX 工作流引擎支持在运行时动态生成步骤，根据数据或条件创建不同的步骤序列。

### 5.1 使用 StepGenerator 动态生成步骤

```kotlin
// 定义步骤生成器
class DataProcessingStepGenerator : StepGenerator {
    override fun generateSteps(context: WorkflowContext): List<WorkflowStep> {
        val dataType = context.getVariable("dataType") as? String ?: "unknown"
        val steps = mutableListOf<WorkflowStep>()
        
        // 根据数据类型生成不同的处理步骤
        when (dataType) {
            "text" -> {
                steps.add(createTextProcessingStep())
                steps.add(createTextAnalysisStep())
            }
            "image" -> {
                steps.add(createImageProcessingStep())
                steps.add(createImageAnalysisStep())
            }
            "audio" -> {
                steps.add(createAudioProcessingStep())
                steps.add(createAudioAnalysisStep())
            }
            else -> {
                steps.add(createGenericProcessingStep())
            }
        }
        
        return steps
    }
    
    private fun createTextProcessingStep(): WorkflowStep {
        // 创建文本处理步骤
    }
    
    private fun createTextAnalysisStep(): WorkflowStep {
        // 创建文本分析步骤
    }
    
    // 其他步骤创建方法...
}

// 在工作流中使用步骤生成器
val dynamicWorkflow = workflow {
    name = "dynamic-step-workflow"
    description = "Workflow with dynamically generated steps"
    
    step(dataTypeDetectionAgent) {
        id = "data_type_detection"
        name = "Data Type Detection"
        description = "Detect type of input data"
    }
    
    // 使用动态步骤生成器
    dynamicSteps {
        id = "data_processing"
        name = "Data Processing"
        description = "Dynamically generated data processing steps"
        after("data_type_detection")
        generator = DataProcessingStepGenerator()
        variables = mapOf(
            "dataType" to variable("$.steps.data_type_detection.output.dataType"),
            "data" to variable("$.steps.data_type_detection.output.data")
        )
    }
    
    step(resultSummaryAgent) {
        id = "result_summary"
        name = "Result Summary"
        description = "Summarize processing results"
        after("data_processing")
    }
}
```

### 5.2 使用条件步骤

```kotlin
// 在工作流中使用条件步骤
val conditionalWorkflow = workflow {
    name = "conditional-workflow"
    description = "Workflow with conditional steps"
    
    step(dataLoadingAgent) {
        id = "data_loading"
        name = "Data Loading"
        description = "Load data from source"
    }
    
    // 条件步骤：根据数据大小选择处理方式
    conditionalStep {
        id = "processing_selection"
        name = "Processing Selection"
        description = "Select processing method based on data size"
        after("data_loading")
        
        condition {
            val dataSize = context.getVariable("$.steps.data_loading.output.dataSize") as? Int ?: 0
            dataSize > 1000
        }
        
        // 大数据处理路径
        onTrue {
            step(batchProcessingAgent) {
                id = "batch_processing"
                name = "Batch Processing"
                description = "Process data in batches"
                variables = mapOf(
                    "data" to variable("$.steps.data_loading.output.data"),
                    "batchSize" to 100
                )
            }
        }
        
        // 小数据处理路径
        onFalse {
            step(directProcessingAgent) {
                id = "direct_processing"
                name = "Direct Processing"
                description = "Process data directly"
                variables = mapOf(
                    "data" to variable("$.steps.data_loading.output.data")
                )
            }
        }
    }
    
    // 结果汇总步骤
    step(resultSummaryAgent) {
        id = "result_summary"
        name = "Result Summary"
        description = "Summarize processing results"
        after("processing_selection")
        variables = mapOf(
            "batchResults" to variable("$.steps.batch_processing.output.results", optional = true),
            "directResults" to variable("$.steps.direct_processing.output.results", optional = true)
        )
    }
}
```

## 6. 最佳实践

### 6.1 子工作流设计原则

- **单一职责**：每个子工作流应该专注于一个特定的任务或功能
- **明确接口**：定义清晰的输入和输出映射，避免隐式依赖
- **适当隔离**：根据需要选择合适的上下文隔离级别
- **错误处理**：为子工作流配置适当的错误处理策略

### 6.2 工作流模板设计原则

- **参数化**：将可变部分抽取为参数，提高模板的可重用性
- **默认值**：为参数提供合理的默认值，简化模板使用
- **验证**：为参数添加验证规则，确保模板使用正确
- **文档**：为模板和参数提供详细的文档，帮助使用者理解

### 6.3 动态工作流性能考虑

- **缓存模板**：频繁使用的工作流模板应该被缓存，避免重复创建
- **延迟生成**：只在需要时动态生成步骤，避免不必要的计算
- **限制复杂度**：避免过于复杂的动态工作流，以保持可维护性

## 7. 示例

### 7.1 数据处理管道

```kotlin
// 定义数据处理子工作流
val dataProcessingSubworkflow = workflow {
    name = "data-processing"
    description = "Process data from various sources"
    
    step(dataLoadingAgent) {
        id = "data_loading"
        name = "Data Loading"
        description = "Load data from source"
        variables = mapOf(
            "source" to variable("$.input.source"),
            "format" to variable("$.input.format")
        )
    }
    
    step(dataCleaningAgent) {
        id = "data_cleaning"
        name = "Data Cleaning"
        description = "Clean loaded data"
        after("data_loading")
        variables = mapOf(
            "data" to variable("$.steps.data_loading.output.data")
        )
    }
    
    step(dataTransformationAgent) {
        id = "data_transformation"
        name = "Data Transformation"
        description = "Transform cleaned data"
        after("data_cleaning")
        variables = mapOf(
            "data" to variable("$.steps.data_cleaning.output.cleanedData")
        )
    }
}

// 定义数据分析子工作流
val dataAnalysisSubworkflow = workflow {
    name = "data-analysis"
    description = "Analyze processed data"
    
    step(statisticalAnalysisAgent) {
        id = "statistical_analysis"
        name = "Statistical Analysis"
        description = "Perform statistical analysis"
        variables = mapOf(
            "data" to variable("$.input.data")
        )
    }
    
    step(patternRecognitionAgent) {
        id = "pattern_recognition"
        name = "Pattern Recognition"
        description = "Recognize patterns in data"
        after("statistical_analysis")
        variables = mapOf(
            "data" to variable("$.input.data"),
            "statistics" to variable("$.steps.statistical_analysis.output.statistics")
        )
    }
    
    step(insightGenerationAgent) {
        id = "insight_generation"
        name = "Insight Generation"
        description = "Generate insights from analysis"
        after("pattern_recognition")
        variables = mapOf(
            "statistics" to variable("$.steps.statistical_analysis.output.statistics"),
            "patterns" to variable("$.steps.pattern_recognition.output.patterns")
        )
    }
}

// 定义主工作流，组合子工作流
val dataAnalyticsPipeline = workflow {
    name = "data-analytics-pipeline"
    description = "End-to-end data analytics pipeline"
    
    step(configurationAgent) {
        id = "configuration"
        name = "Configuration"
        description = "Configure the analytics pipeline"
    }
    
    // 使用数据处理子工作流
    subworkflow {
        id = "data_processing"
        name = "Data Processing"
        description = "Process data using subworkflow"
        after("configuration")
        workflow = dataProcessingSubworkflow
        inputMapping = mapOf(
            "source" to variable("$.steps.configuration.output.dataSource"),
            "format" to variable("$.steps.configuration.output.dataFormat")
        )
        outputMapping = mapOf(
            "processedData" to variable("$.steps.data_transformation.output.transformedData")
        )
    }
    
    // 使用数据分析子工作流
    subworkflow {
        id = "data_analysis"
        name = "Data Analysis"
        description = "Analyze data using subworkflow"
        after("data_processing")
        workflow = dataAnalysisSubworkflow
        inputMapping = mapOf(
            "data" to variable("$.steps.data_processing.output.processedData")
        )
        outputMapping = mapOf(
            "insights" to variable("$.steps.insight_generation.output.insights")
        )
    }
    
    step(reportGenerationAgent) {
        id = "report_generation"
        name = "Report Generation"
        description = "Generate analytics report"
        after("data_analysis")
        variables = mapOf(
            "insights" to variable("$.steps.data_analysis.output.insights"),
            "format" to variable("$.steps.configuration.output.reportFormat")
        )
    }
}
```

### 7.2 动态数据处理工作流

```kotlin
// 定义动态数据处理工作流生成器
class DataProcessingWorkflowGenerator {
    fun generateWorkflow(dataSource: String, dataType: String, processingOptions: Map<String, Any>): Workflow {
        val workflowBuilder = WorkflowBuilder()
            .withName("dynamic-data-processing")
            .withDescription("Dynamically generated data processing workflow")
        
        // 添加数据加载步骤
        workflowBuilder.addStep(
            StepBuilder()
                .withId("data_loading")
                .withName("Data Loading")
                .withDescription("Load data from source")
                .withAgent(getDataLoadingAgent(dataSource))
                .build()
        )
        
        // 根据数据类型添加处理步骤
        val processingSteps = generateProcessingSteps(dataType, processingOptions)
        processingSteps.forEach { step ->
            workflowBuilder.addStep(step)
        }
        
        // 添加结果导出步骤
        workflowBuilder.addStep(
            StepBuilder()
                .withId("result_export")
                .withName("Result Export")
                .withDescription("Export processing results")
                .withAgent(getResultExportAgent(processingOptions["outputFormat"] as? String))
                .withAfter(processingSteps.last().id)
                .build()
        )
        
        return workflowBuilder.build()
    }
    
    private fun getDataLoadingAgent(dataSource: String): Agent {
        // 根据数据源类型返回合适的数据加载代理
        return when {
            dataSource.startsWith("s3://") -> s3DataLoadingAgent
            dataSource.startsWith("http://") || dataSource.startsWith("https://") -> httpDataLoadingAgent
            dataSource.startsWith("file://") -> fileDataLoadingAgent
            else -> defaultDataLoadingAgent
        }
    }
    
    private fun generateProcessingSteps(dataType: String, options: Map<String, Any>): List<WorkflowStep> {
        // 根据数据类型和选项生成处理步骤
        return when (dataType) {
            "text" -> generateTextProcessingSteps(options)
            "image" -> generateImageProcessingSteps(options)
            "audio" -> generateAudioProcessingSteps(options)
            "video" -> generateVideoProcessingSteps(options)
            else -> generateGenericProcessingSteps(options)
        }
    }
    
    private fun getResultExportAgent(outputFormat: String?): Agent {
        // 根据输出格式返回合适的结果导出代理
        return when (outputFormat) {
            "json" -> jsonExportAgent
            "xml" -> xmlExportAgent
            "csv" -> csvExportAgent
            else -> defaultExportAgent
        }
    }
    
    // 其他辅助方法...
}

// 使用动态工作流生成器
val generator = DataProcessingWorkflowGenerator()
val workflow = generator.generateWorkflow(
    dataSource = "s3://data-bucket/images",
    dataType = "image",
    processingOptions = mapOf(
        "resolution" to "high",
        "colorMode" to "rgb",
        "outputFormat" to "json"
    )
)

// 执行动态生成的工作流
workflowEngine.execute(workflow, input)
```

## 8. 总结

KastraX 工作流引擎提供了强大的动态工作流和子工作流功能，允许开发者创建灵活、可重用和可组合的工作流。通过动态工作流创建，开发者可以根据条件或数据动态调整工作流结构；通过子工作流，开发者可以将复杂的工作流分解为更小、更可管理的部分；通过工作流模板，开发者可以创建可重用的工作流定义，提高开发效率。这些功能使 KastraX 工作流引擎能够支持各种复杂的工作流场景，提高工作流系统的灵活性和可扩展性。
