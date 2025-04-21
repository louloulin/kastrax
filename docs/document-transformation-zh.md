# 文档转换功能

## 1. 概述

文档转换是 KastraX RAG 系统的重要组成部分，它负责将各种格式的文档转换为更适合处理和分析的格式。本文档详细介绍了 KastraX 中实现的文档转换功能，包括文本清理和规范化、HTML 到纯文本的转换以及表格数据提取。

## 2. 文本清理器 (TextCleaner)

### 2.1 功能介绍

文本清理器用于清理和规范化文本内容，提高文本质量和一致性。它提供了多种文本处理选项，可以根据需要进行配置。

### 2.2 主要功能

- **空白处理**：去除首尾空白、规范化空白字符、移除多余空格
- **标点规范化**：统一引号、破折号、省略号等标点符号
- **大小写转换**：将文本转换为小写或大写
- **特殊字符处理**：移除特殊字符、规范化 Unicode 字符
- **结构处理**：移除空行、规范化行尾
- **内容过滤**：移除 URL、HTML 标签、数字等
- **自定义替换**：支持自定义文本替换规则

### 2.3 使用示例

```kotlin
// 创建文本清理器
val cleaner = TextCleaner()

// 创建清理选项
val options = TextCleaner.CleaningOptions(
    trimWhitespace = true,
    normalizeWhitespace = true,
    normalizePunctuation = true,
    toLowerCase = true,
    removeExtraSpaces = true
)

// 清理文本
val cleanedText = cleaner.clean("  这是一个 测试  文本，包含  多余的空格  和标点符号！  ", options)

// 清理文档
val document = Document("  这是一个测试文档  ", mapOf("source" to "test"))
val cleanedDocument = cleaner.clean(document, options)
```

### 2.4 预设选项

文本清理器提供了几种预设的清理选项，方便快速使用：

- **基本清理**：`TextCleaner.basicCleaningOptions()`
- **标准化文本**：`TextCleaner.standardizationOptions()`
- **搜索引擎优化**：`TextCleaner.seoOptions()`
- **极简文本**：`TextCleaner.minimalTextOptions()`

### 2.5 构建器模式

文本清理器支持构建器模式，方便链式配置清理选项：

```kotlin
val options = TextCleaner.CleaningOptions()
    .toBuilder()
    .trimWhitespace(true)
    .normalizeWhitespace(true)
    .toLowerCase(true)
    .build()
```

## 3. HTML 到文本转换器 (HtmlToTextConverter)

### 3.1 功能介绍

HTML 到文本转换器用于将 HTML 文档转换为纯文本，同时尽可能保留文本结构和格式信息。它使用 Jsoup 库解析 HTML，并提供多种选项来控制转换过程。

### 3.2 主要功能

- **结构保留**：保留标题、段落、列表等结构
- **格式保留**：可选择保留文本格式（粗体、斜体等）
- **链接处理**：可选择保留链接信息
- **表格处理**：可选择保留表格结构
- **图片处理**：提取图片的 alt 文本
- **内容过滤**：移除脚本、样式和隐藏元素

### 3.3 使用示例

```kotlin
// 创建 HTML 到文本转换器
val converter = HtmlToTextConverter()

// 创建转换选项
val options = HtmlToTextConverter.ConversionOptions(
    preserveLineBreaks = true,
    preserveHeaderFormatting = true,
    preserveTextFormatting = true,
    preserveLinks = true,
    preserveTableStructure = true
)

// 转换 HTML 字符串
val html = "<h1>标题</h1><p>这是<b>粗体</b>文本。</p>"
val text = converter.convert(html, options)

// 转换 HTML 文档
val document = Document(html, mapOf("source" to "test.html"))
val convertedDocument = converter.convert(document, options)
```

### 3.4 预设选项

HTML 到文本转换器提供了几种预设的转换选项：

- **简单转换**：`HtmlToTextConverter.simpleConversionOptions()`
- **结构保留**：`HtmlToTextConverter.structurePreservingOptions()`
- **Markdown 转换**：`HtmlToTextConverter.markdownConversionOptions()`

### 3.5 转换格式

根据配置的不同，HTML 到文本转换器可以生成不同格式的文本：

- **纯文本**：简单的纯文本，不包含任何格式信息
- **结构化文本**：保留标题、段落、列表等结构，但不包含格式信息
- **Markdown 格式**：保留结构和格式，使用 Markdown 语法表示

## 4. 表格提取器 (TableExtractor)

### 4.1 功能介绍

表格提取器用于从 HTML、JSON 和其他格式中提取表格数据，并将其转换为结构化格式。它支持多种输入格式和输出格式，方便在不同场景中使用。

### 4.2 主要功能

- **多源提取**：支持从 HTML、JSON 等格式中提取表格
- **结构化输出**：将表格数据转换为结构化格式
- **多格式输出**：支持 CSV、Markdown、JSON、HTML 等输出格式
- **元数据提取**：提取表格相关的元数据

### 4.3 使用示例

```kotlin
// 创建表格提取器
val extractor = TableExtractor()

// 从 HTML 中提取表格
val html = """
    <table>
        <tr><th>姓名</th><th>年龄</th></tr>
        <tr><td>张三</td><td>30</td></tr>
        <tr><td>李四</td><td>25</td></tr>
    </table>
"""
val tables = extractor.extractFromHtml(html)

// 从 JSON 中提取表格
val json = """
    {
        "name": "用户表",
        "headers": ["姓名", "年龄"],
        "rows": [
            ["张三", "30"],
            ["李四", "25"]
        ]
    }
"""
val jsonTables = extractor.extractFromJson(json)

// 将表格转换为不同格式
val table = tables.first()
val csv = table.toCsv()
val markdown = table.toMarkdown()
val jsonOutput = table.toJson()
val htmlOutput = table.toHtml()

// 将表格转换为文档
val document = table.toDocument("markdown")
```

### 4.4 表格格式

表格提取器支持多种表格格式：

- **HTML 表格**：从 HTML 文档中提取的表格
- **JSON 表格**：包含表头和行数据的 JSON 对象
- **对象数组**：每个对象代表一行数据的 JSON 数组

### 4.5 输出格式

表格提取器支持多种输出格式：

- **CSV**：逗号分隔的文本格式
- **Markdown**：Markdown 表格格式
- **JSON**：JSON 格式的表格数据
- **HTML**：HTML 表格格式

## 5. 文档转换器 (DocumentTransformer)

### 5.1 功能介绍

文档转换器是一个通用接口，用于转换文档。它定义了转换单个文档和多个文档的方法，方便在 RAG 系统中使用。

### 5.2 主要实现

- **TextCleaningTransformer**：使用 TextCleaner 清理文档文本
- **HtmlToTextTransformer**：使用 HtmlToTextConverter 将 HTML 文档转换为纯文本
- **TableExtractionTransformer**：使用 TableExtractor 从文档中提取表格
- **CompositeTransformer**：组合多个转换器，按顺序应用

### 5.3 使用示例

```kotlin
// 创建文本清理转换器
val textCleaner = TextCleaningTransformer(
    options = TextCleaner.standardizationOptions()
)

// 创建 HTML 到文本转换器
val htmlToText = HtmlToTextTransformer(
    options = HtmlToTextConverter.markdownConversionOptions()
)

// 创建表格提取转换器
val tableExtractor = TableExtractionTransformer(
    outputFormat = "markdown",
    extractAsDocuments = true
)

// 创建复合转换器
val transformer = CompositeTransformer(
    htmlToText,
    textCleaner
)

// 转换文档
val document = Document("<h1>标题</h1><p>这是<b>粗体</b>文本。</p>")
val transformedDocument = transformer.transform(document)

// 转换多个文档
val documents = listOf(document1, document2, document3)
val transformedDocuments = transformer.transform(documents)
```

## 6. 最佳实践

### 6.1 文本清理

- 对于一般文本处理，使用 `TextCleaner.standardizationOptions()`
- 对于搜索优化，使用 `TextCleaner.seoOptions()`
- 对于需要保留原始格式的场景，只使用必要的清理选项

### 6.2 HTML 转换

- 对于需要保留格式的场景，使用 `HtmlToTextConverter.markdownConversionOptions()`
- 对于只需要内容的场景，使用 `HtmlToTextConverter.simpleConversionOptions()`
- 对于需要结构但不需要格式的场景，使用 `HtmlToTextConverter.structurePreservingOptions()`

### 6.3 表格提取

- 对于简单表格，直接使用 `extractFromHtml` 或 `extractFromJson`
- 对于复杂表格，考虑使用自定义提取选项
- 根据需要选择合适的输出格式（CSV、Markdown、JSON、HTML）

### 6.4 组合使用

- 使用 `CompositeTransformer` 组合多个转换器
- 注意转换器的顺序，通常先进行格式转换，再进行文本清理
- 对于特定需求，可以实现自定义的 `DocumentTransformer`

## 7. 总结

文档转换功能是 KastraX RAG 系统的重要组成部分，它提供了丰富的工具来处理和转换各种格式的文档。通过文本清理、HTML 转换和表格提取，可以将原始文档转换为更适合处理和分析的格式，提高 RAG 系统的效果。

这些功能设计灵活，可以根据不同的需求进行配置和组合，满足各种场景的需求。同时，它们也提供了良好的错误处理机制，确保系统的稳定性和可靠性。
