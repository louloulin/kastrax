# kastrax 修复计划 (xf2.md)

## 实现进度

### 已完成
- [x] 创建缺失的 SemanticMemorySearchResult 类
- [x] 修复 SymbolRelationGraph.kt 中的重复 SymbolRelationGraphConfig 类
- [x] 创建 SymbolType 枚举类
- [x] 创建 Pattern 接口和设计模式类
- [x] 修复 SymbolRelationGraphBuilder.kt 中的 addRelation 方法调用
- [x] 添加 CodeRelationAnalyzer 导入
- [x] 添加 convertToSymbolType 方法
- [x] 解决 PatternCategory 和 PatternMatch 类的重复声明问题
- [x] 修复 SymbolRelationGraph.kt 中的方法重载冲突
- [x] 修复 SymbolRelationGraph.kt 中的 convertToSymbolType 方法调用问题

### 部分完成
- [部分] 修复 HybridRetriever.kt 中的类型转换问题
  - [x] 修复 cache 类型为 ConcurrentHashMap<String, List<HybridRetrievalResult>>
  - [x] 修复 rerank 和 rerankHybrid 方法的参数类型
  - [部分] 修复 result.element 和 result.score 引用问题
  - [部分] 修复 toRetrievalResult 方法调用问题
  - [ ] 修复类型不匹配问题：实际类型是 List<RetrievalResult>，期望类型是 List<HybridRetrievalResult>

- [部分] 修复 DesignPatternDetector.kt 中的未解析引用
  - [x] 修复类继承结构
  - [x] 添加模式检测方法
  - [x] 修复 PatternMatch 构造函数参数问题
  - [x] 修复字符串与布尔值比较问题
  - [x] 修复 detectPatternsFromFlowGraph 方法的冲突
  - [x] 修复 Type inference failed 问题

### 待解决问题
- [x] 修复 DesignPatternDetector.kt 中的 model 引用问题
- [x] 修复 HybridRetriever.kt 中的 combinedScore、vectorScore 和 keywordScore 引用问题
- [x] 修复 SymbolRelationGraphBuilder.kt 中的 VARIABLE、NAMESPACE、MODULE 常量引用
- [x] 修复 SymbolRelationGraph.kt 中的 VARIABLE、NAMESPACE、MODULE 常量引用

## 下一步计划
1. 修复 HybridRetriever.kt 中的 combinedScore、vectorScore 和 keywordScore 引用问题
2. 修复测试代码中的类型不匹配问题
3. 修复测试代码中的协程调用问题
4. 修复 ChapiCodeParserTest.kt 中的未解析引用问题
5. 添加测试验证

## 问题概述

执行 `gradle build` 时，发现主要问题集中在 `kastrax-codebase` 模块中，具体表现为编译错误。主要错误包括：

1. 类型不匹配错误：在 `HybridRetriever.kt` 中，存在参数类型不匹配问题
2. 无法推断类型参数：在多个文件中存在类型推断问题
3. 未解析的引用：多个文件中存在未解析的引用，如 `score`、`id`、`getNodes` 等
4. 类型转换问题：在 `SymbolRelationGraphBuilder.kt` 中存在类型转换问题
5. 缺少 `SemanticMemorySearchResult` 类的引用
6. 操作符应用问题：在 `DesignPatternDetector.kt` 中存在操作符应用问题

## 详细问题分析

### 1. HybridRetriever.kt 文件问题

- 第148行和第152行：参数类型不匹配，实际类型是 `List<Any>`，但期望类型是 `List<HybridRetrievalResult>`
- 第364、367、370、377行：无法推断参数类型，需要显式指定
- 第364、367、370、377行：未解析的引用 `score`
- 第379行：未解析的引用 `id`

### 2. RetrievalModel.kt 文件问题

- 第58行：未解析的引用 `SemanticMemorySearchResult`
- 第58行：无法推断参数类型，需要显式指定

### 3. DesignPatternDetector.kt 文件问题

- 第8行：未解析的引用 `model`
- 第367、375、383、410、416、424、451、457、465行：未解析的引用 `getNodes` 或 `getEdges`
- 第367、375、416、457行：无法推断参数类型
- 第368、376、417、458行：未解析的引用 `element`
- 第384、385、425、426、466、467行：未解析的引用 `it`
- 第411、452行：未解析的引用 `metadata`
- 第412、453行：操作符 `==` 不能应用于 `kotlin.text.MatchGroup?` 和 `kotlin.Boolean`
- 第454行：需要 `operator` 修饰符

### 4. SymbolRelationGraphBuilder.kt 文件问题

- 第189行：未解析的引用 `getElementById`
- 第192、201行：参数类型不匹配，实际类型是 `kotlin.Any`，但期望类型是 `ai.kastrax.codebase.semantic.model.CodeElement`

### 5. builder/SymbolRelationGraphBuilder.kt 文件问题

- 第51行：未解析的引用 `addRelation`
- 第114、117、118行：未解析的引用 `VARIABLE`、`NAMESPACE`、`MODULE`
- 第156-177行：多个未解析的引用，如 `includeFiles`、`includePackages` 等

### 6. SymbolRelationGraph.kt 文件问题

- 第428行：未解析的引用 `convertToSymbolType`
- 第509、512、513行：未解析的引用 `VARIABLE`、`NAMESPACE`、`MODULE`
- 第579行：未解析的引用 `relations`

## 修复方案

### 1. 创建缺失的类和方法

1. 创建 `HybridRetrievalResult` 类：
   - 在 `ai.kastrax.codebase.retrieval.model` 包中创建 `HybridRetrievalResult.kt` 文件
   - 实现必要的属性和方法，包括 `id`、`element`、`vectorScore`、`keywordScore`、`combinedScore`、`source` 等
   - 实现 `toRetrievalResult()` 方法

2. 创建 `SemanticMemorySearchResult` 类：
   - 在 `ai.kastrax.codebase.semantic.memory` 包中创建或修复此类

3. 修复 `FlowGraph` 类：
   - 添加 `getNodes()` 和 `getEdges()` 方法

### 2. 修复类型不匹配问题

1. 修复 `HybridRetriever.kt` 中的类型转换问题：
   - 第148行和第152行：确保返回类型为 `List<HybridRetrievalResult>`
   - 修复 `rerank` 和 `rerankHybrid` 方法中的类型问题

2. 修复 `SymbolRelationGraphBuilder.kt` 中的类型转换问题：
   - 确保 `getElementById` 方法返回 `CodeElement` 类型

### 3. 修复未解析的引用

1. 在 `HybridRetriever.kt` 中：
   - 修复 `score` 引用，可能需要使用 `combinedScore` 或其他属性
   - 修复 `id` 引用，可能需要使用 `element.id`

2. 在 `DesignPatternDetector.kt` 中：
   - 添加缺失的 `model` 导入
   - 修复 `getNodes` 和 `getEdges` 方法调用
   - 修复 `element` 和 `it` 引用
   - 修复 `metadata` 引用
   - 修复操作符比较问题

3. 在 `SymbolRelationGraph.kt` 中：
   - 修复 `relations` 引用，可能应该是 `edges`

### 4. 修复类型推断问题

1. 在需要显式指定类型的地方添加类型参数：
   - 例如，在 `sortedByDescending` 调用中指定比较器类型

## 实施步骤

1. 首先创建缺失的类和方法
2. 修复类型不匹配问题
3. 修复未解析的引用
4. 修复类型推断问题
5. 运行单元测试确保修复有效
6. 重新执行 `gradle build` 验证修复结果

## 优先级

1. 创建 `HybridRetrievalResult` 类（高优先级）
2. 修复 `HybridRetriever.kt` 中的问题（高优先级）
3. 修复 `DesignPatternDetector.kt` 中的问题（中优先级）
4. 修复 `SymbolRelationGraphBuilder.kt` 和 `SymbolRelationGraph.kt` 中的问题（中优先级）
5. 修复 `RetrievalModel.kt` 中的问题（低优先级）

## 后续工作

1. 完善单元测试，确保修复后的代码能够正常工作
2. 重构代码，提高可维护性
3. 更新文档，说明修复的内容和影响
4. 考虑添加更多的类型安全检查，避免类似问题再次发生
