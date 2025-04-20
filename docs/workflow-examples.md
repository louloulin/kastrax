# 工作流引擎使用示例

本文档提供了一系列工作流引擎的使用示例，从简单到复杂，帮助开发者理解如何使用工作流引擎构建各种 AI 应用。

## 基础示例：简单顺序工作流

这个示例展示了一个简单的顺序工作流，包含两个步骤：

```kotlin
val simpleWorkflow = workflow {
    name = "simple-workflow"
    description = "A simple sequential workflow"

    step(researchAgent) {
        id = "research"
        name = "Research"
        description = "Research a topic"
        variables = mapOf(
            "topic" to variable("$.input.topic")
        )
    }

    step(summaryAgent) {
        id = "summary"
        name = "Summary"
        description = "Summarize the research"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text")
        )
    }
}

// 执行工作流
val input = mapOf("topic" to "Artificial Intelligence")
val result = simpleWorkflow.execute(input)
```

## 中级示例：内容创作工作流

这个示例展示了一个内容创作工作流，包含研究、写作和编辑三个步骤：

```kotlin
val contentCreationWorkflow = workflow {
    name = "content-creation"
    description = "Create content about a topic"

    step(researchAgent) {
        id = "research"
        name = "Research"
        description = "Research the topic"
        variables = mapOf(
            "topic" to variable("$.input.topic")
        )
    }

    step(writingAgent) {
        id = "writing"
        name = "Writing"
        description = "Write an article based on research"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text"),
            "style" to variable("$.input.style")
        )
    }

    step(editingAgent) {
        id = "editing"
        name = "Editing"
        description = "Edit the article"
        after("writing")
        variables = mapOf(
            "draft" to variable("$.steps.writing.output.text"),
            "guidelines" to variable("$.input.guidelines")
        )
    }
}

// 执行工作流
val input = mapOf(
    "topic" to "Artificial Intelligence",
    "style" to "Informative",
    "guidelines" to "Ensure clarity and accuracy"
)
val result = contentCreationWorkflow.execute(input)
```

## 高级示例：数据分析与报告生成工作流

这个示例展示了一个数据分析与报告生成工作流，包含数据收集、数据分析、可视化建议和报告生成四个步骤：

```kotlin
val dataAnalysisWorkflow = workflow {
    name = "data-analysis"
    description = "Analyze data and generate a report"

    step(dataCollectionAgent) {
        id = "data_collection"
        name = "Data Collection"
        description = "Collect data from sources"
        variables = mapOf(
            "topic" to variable("$.input.topic"),
            "sources" to variable("$.input.sources")
        )
    }

    step(dataAnalysisAgent) {
        id = "data_analysis"
        name = "Data Analysis"
        description = "Analyze collected data"
        after("data_collection")
        variables = mapOf(
            "data" to variable("$.steps.data_collection.output.text")
        )
    }

    step(visualizationAgent) {
        id = "visualization"
        name = "Visualization"
        description = "Suggest visualizations for the data"
        after("data_analysis")
        variables = mapOf(
            "analysis" to variable("$.steps.data_analysis.output.text")
        )
    }

    step(reportGenerationAgent) {
        id = "report_generation"
        name = "Report Generation"
        description = "Generate a report based on analysis and visualizations"
        after("data_analysis", "visualization")
        variables = mapOf(
            "analysis" to variable("$.steps.data_analysis.output.text"),
            "visualizations" to variable("$.steps.visualization.output.text"),
            "format" to variable("$.input.format")
        )
        outputMapping = { text ->
            // 保存报告到文件
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val fileName = "report_${timestamp}.md"
            val filePath = "reports/$fileName"
            
            // 确保目录存在
            File("reports").mkdirs()
            
            // 写入文件
            File(filePath).writeText(text)
            
            mapOf(
                "text" to text,
                "filePath" to filePath
            )
        }
    }
}

// 执行工作流
val input = mapOf(
    "topic" to "Climate Change",
    "sources" to listOf("IPCC Reports", "NASA Data", "NOAA Data"),
    "format" to "Markdown"
)
val result = dataAnalysisWorkflow.execute(input)
```

## 复杂示例：产品开发工作流

这个示例展示了一个产品开发工作流，包含市场研究、需求分析、产品设计、原型开发和用户测试五个步骤：

```kotlin
val productDevelopmentWorkflow = workflow {
    name = "product-development"
    description = "Develop a new product from concept to prototype"

    step(marketResearchAgent) {
        id = "market_research"
        name = "Market Research"
        description = "Research the market and competitors"
        variables = mapOf(
            "product_category" to variable("$.input.product_category"),
            "target_audience" to variable("$.input.target_audience")
        )
    }

    step(requirementsAgent) {
        id = "requirements"
        name = "Requirements Analysis"
        description = "Analyze requirements based on market research"
        after("market_research")
        variables = mapOf(
            "market_research" to variable("$.steps.market_research.output.text"),
            "business_goals" to variable("$.input.business_goals")
        )
    }

    step(designAgent) {
        id = "design"
        name = "Product Design"
        description = "Design the product based on requirements"
        after("requirements")
        variables = mapOf(
            "requirements" to variable("$.steps.requirements.output.text"),
            "design_guidelines" to variable("$.input.design_guidelines")
        )
    }

    step(prototypeAgent) {
        id = "prototype"
        name = "Prototype Development"
        description = "Develop a prototype based on the design"
        after("design")
        variables = mapOf(
            "design" to variable("$.steps.design.output.text"),
            "technical_constraints" to variable("$.input.technical_constraints")
        )
    }

    step(userTestingAgent) {
        id = "user_testing"
        name = "User Testing"
        description = "Plan user testing for the prototype"
        after("prototype")
        variables = mapOf(
            "prototype" to variable("$.steps.prototype.output.text"),
            "testing_goals" to variable("$.input.testing_goals")
        )
    }
}

// 执行工作流
val input = mapOf(
    "product_category" to "Smart Home Devices",
    "target_audience" to "Tech-savvy homeowners, age 30-50",
    "business_goals" to "Increase market share by 10% in the next year",
    "design_guidelines" to "Modern, minimalist design with intuitive user interface",
    "technical_constraints" to "Must be compatible with existing smart home ecosystems",
    "testing_goals" to "Evaluate usability and user satisfaction"
)
val result = productDevelopmentWorkflow.execute(input)
```

## 并行步骤示例：新闻聚合工作流

这个示例展示了一个新闻聚合工作流，包含多个并行的数据收集步骤和一个聚合步骤：

```kotlin
val newsAggregationWorkflow = workflow {
    name = "news-aggregation"
    description = "Aggregate news from multiple sources"

    step(techNewsAgent) {
        id = "tech_news"
        name = "Tech News"
        description = "Collect technology news"
        variables = mapOf(
            "topic" to variable("$.input.topic"),
            "sources" to variable("$.input.tech_sources")
        )
    }

    step(businessNewsAgent) {
        id = "business_news"
        name = "Business News"
        description = "Collect business news"
        variables = mapOf(
            "topic" to variable("$.input.topic"),
            "sources" to variable("$.input.business_sources")
        )
    }

    step(scienceNewsAgent) {
        id = "science_news"
        name = "Science News"
        description = "Collect science news"
        variables = mapOf(
            "topic" to variable("$.input.topic"),
            "sources" to variable("$.input.science_sources")
        )
    }

    step(aggregationAgent) {
        id = "aggregation"
        name = "News Aggregation"
        description = "Aggregate news from all sources"
        after("tech_news", "business_news", "science_news")
        variables = mapOf(
            "tech_news" to variable("$.steps.tech_news.output.text"),
            "business_news" to variable("$.steps.business_news.output.text"),
            "science_news" to variable("$.steps.science_news.output.text"),
            "format" to variable("$.input.format")
        )
    }
}

// 执行工作流
val input = mapOf(
    "topic" to "Artificial Intelligence",
    "tech_sources" to listOf("TechCrunch", "Wired", "The Verge"),
    "business_sources" to listOf("Forbes", "Bloomberg", "Wall Street Journal"),
    "science_sources" to listOf("Nature", "Science", "MIT Technology Review"),
    "format" to "Newsletter"
)
val result = newsAggregationWorkflow.execute(input)
```

## 条件执行示例：客户支持工作流

这个示例展示了一个客户支持工作流，根据问题类型执行不同的步骤：

```kotlin
val customerSupportWorkflow = workflow {
    name = "customer-support"
    description = "Handle customer support requests"

    step(classificationAgent) {
        id = "classification"
        name = "Request Classification"
        description = "Classify the customer request"
        variables = mapOf(
            "request" to variable("$.input.request"),
            "customer_info" to variable("$.input.customer_info")
        )
        outputMapping = { text ->
            // 解析分类结果
            val type = when {
                text.contains("technical", ignoreCase = true) -> "technical"
                text.contains("billing", ignoreCase = true) -> "billing"
                text.contains("account", ignoreCase = true) -> "account"
                else -> "general"
            }
            
            mapOf(
                "text" to text,
                "type" to type
            )
        }
    }

    step(technicalSupportAgent) {
        id = "technical_support"
        name = "Technical Support"
        description = "Handle technical support requests"
        after("classification")
        variables = mapOf(
            "request" to variable("$.input.request"),
            "classification" to variable("$.steps.classification.output.text"),
            "type" to variable("$.steps.classification.output.type")
        )
        // 只有当问题类型为技术问题时才执行
        condition = { context ->
            val type = context.steps["classification"]?.output?.get("type") as? String
            type == "technical"
        }
    }

    step(billingSupportAgent) {
        id = "billing_support"
        name = "Billing Support"
        description = "Handle billing support requests"
        after("classification")
        variables = mapOf(
            "request" to variable("$.input.request"),
            "classification" to variable("$.steps.classification.output.text"),
            "type" to variable("$.steps.classification.output.type"),
            "customer_info" to variable("$.input.customer_info")
        )
        // 只有当问题类型为账单问题时才执行
        condition = { context ->
            val type = context.steps["classification"]?.output?.get("type") as? String
            type == "billing"
        }
    }

    step(accountSupportAgent) {
        id = "account_support"
        name = "Account Support"
        description = "Handle account support requests"
        after("classification")
        variables = mapOf(
            "request" to variable("$.input.request"),
            "classification" to variable("$.steps.classification.output.text"),
            "type" to variable("$.steps.classification.output.type"),
            "customer_info" to variable("$.input.customer_info")
        )
        // 只有当问题类型为账户问题时才执行
        condition = { context ->
            val type = context.steps["classification"]?.output?.get("type") as? String
            type == "account"
        }
    }

    step(generalSupportAgent) {
        id = "general_support"
        name = "General Support"
        description = "Handle general support requests"
        after("classification")
        variables = mapOf(
            "request" to variable("$.input.request"),
            "classification" to variable("$.steps.classification.output.text"),
            "type" to variable("$.steps.classification.output.type")
        )
        // 只有当问题类型为一般问题时才执行
        condition = { context ->
            val type = context.steps["classification"]?.output?.get("type") as? String
            type == "general"
        }
    }

    step(responseFormattingAgent) {
        id = "response_formatting"
        name = "Response Formatting"
        description = "Format the final response to the customer"
        after("technical_support", "billing_support", "account_support", "general_support")
        variables = mapOf(
            "technical_response" to variable("$.steps.technical_support.output.text"),
            "billing_response" to variable("$.steps.billing_support.output.text"),
            "account_response" to variable("$.steps.account_support.output.text"),
            "general_response" to variable("$.steps.general_support.output.text"),
            "type" to variable("$.steps.classification.output.type"),
            "customer_info" to variable("$.input.customer_info"),
            "tone" to variable("$.input.tone")
        )
    }
}

// 执行工作流
val input = mapOf(
    "request" to "I'm having trouble logging into my account. It says my password is incorrect, but I'm sure it's right.",
    "customer_info" to mapOf(
        "name" to "John Doe",
        "email" to "john.doe@example.com",
        "account_id" to "A12345",
        "subscription_level" to "Premium"
    ),
    "tone" to "Friendly"
)
val result = customerSupportWorkflow.execute(input)
```

## 总结

这些示例展示了工作流引擎的多种用法，从简单的顺序工作流到复杂的条件执行和并行步骤。通过这些示例，开发者可以了解如何使用工作流引擎构建各种 AI 应用，满足不同的业务需求。

工作流引擎的灵活性和可扩展性使其适用于各种场景，包括但不限于：

- 内容创作和编辑
- 数据分析和报告生成
- 产品开发和设计
- 客户支持和服务
- 新闻聚合和内容策划
- 研究和文献综述
- 教育和培训内容开发

通过组合不同的 AI 代理和工具，开发者可以创建强大的工作流，自动化复杂的知识工作流程。
