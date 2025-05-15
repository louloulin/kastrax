package ai.kastrax.codebase.semantic.memory

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 导入分析器配置
 *
 * @property enableLibraryAnalysis 是否启用库分析
 * @property maxLibraryDepth 最大库依赖深度
 * @property libraryPrefixes 库前缀列表
 * @property standardLibraryPrefixes 标准库前缀列表
 * @property maxConcurrentTasks 最大并发任务数
 */
data class ImportAnalyzerConfig(
    val enableLibraryAnalysis: Boolean = true,
    val maxLibraryDepth: Int = 2,
    val libraryPrefixes: Set<String> = defaultLibraryPrefixes(),
    val standardLibraryPrefixes: Set<String> = defaultStandardLibraryPrefixes(),
    val maxConcurrentTasks: Int = 10
)

/**
 * 默认库前缀
 *
 * @return 库前缀集合
 */
fun defaultLibraryPrefixes(): Set<String> {
    return setOf(
        // Java
        "java.", "javax.", "com.sun.", "org.w3c.", "org.xml.",
        // Kotlin
        "kotlin.", "kotlinx.",
        // Android
        "android.", "androidx.",
        // Spring
        "org.springframework.",
        // Apache
        "org.apache.",
        // Google
        "com.google.",
        // Python
        "numpy", "pandas", "tensorflow", "torch", "sklearn",
        // JavaScript/TypeScript
        "react", "vue", "angular", "express", "lodash", "jquery"
    )
}

/**
 * 默认标准库前缀
 *
 * @return 标准库前缀集合
 */
fun defaultStandardLibraryPrefixes(): Set<String> {
    return setOf(
        // Java
        "java.lang.", "java.util.", "java.io.", "java.nio.", "java.time.",
        // Kotlin
        "kotlin.", "kotlinx.coroutines.", "kotlinx.serialization.",
        // Python
        "os", "sys", "math", "datetime", "collections", "json", "re",
        // JavaScript
        "Object", "Array", "String", "Number", "Math", "Date", "JSON"
    )
}

/**
 * 导入分析结果
 *
 * @property importElement 导入元素
 * @property targetElement 目标元素
 * @property isLibrary 是否为库导入
 * @property isStandardLibrary 是否为标准库导入
 * @property libraryInfo 库信息
 */
data class ImportAnalysisResult(
    val importElement: CodeElement,
    val targetElement: CodeElement? = null,
    val isLibrary: Boolean = false,
    val isStandardLibrary: Boolean = false,
    val libraryInfo: LibraryInfo? = null
)

/**
 * 库信息
 *
 * @property name 库名称
 * @property version 版本
 * @property description 描述
 * @property apiElements API元素列表
 */
data class LibraryInfo(
    val name: String,
    val version: String? = null,
    val description: String? = null,
    val apiElements: List<CodeElement> = emptyList()
)

/**
 * 导入分析器
 *
 * 分析代码中的导入语句和库使用
 *
 * @property config 配置
 */
class ImportAnalyzer(
    private val config: ImportAnalyzerConfig = ImportAnalyzerConfig()
) {
    // 库信息缓存
    private val libraryInfoCache = ConcurrentHashMap<String, LibraryInfo>()

    /**
     * 分析导入语句
     *
     * @param codeElements 代码元素列表
     * @return 导入分析结果列表
     */
    suspend fun analyzeImports(codeElements: List<CodeElement>): List<ImportAnalysisResult> = withContext(Dispatchers.Default) {
        try {
            // 获取所有导入元素
            val importElements = codeElements.flatMap { element ->
                if (element.type == CodeElementType.FILE) {
                    element.children.filter { it.type == CodeElementType.IMPORT }
                } else {
                    emptyList()
                }
            }

            logger.debug { "找到 ${importElements.size} 个导入语句" }

            // 并行分析导入
            val results = coroutineScope {
                importElements.chunked(config.maxConcurrentTasks).flatMap { chunk ->
                    chunk.map { importElement ->
                        async {
                            analyzeImport(importElement, codeElements)
                        }
                    }.awaitAll()
                }
            }

            return@withContext results
        } catch (e: Exception) {
            logger.error(e) { "分析导入语句时出错: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 分析单个导入语句
     *
     * @param importElement 导入元素
     * @param allElements 所有代码元素
     * @return 导入分析结果
     */
    private suspend fun analyzeImport(importElement: CodeElement, allElements: List<CodeElement>): ImportAnalysisResult = withContext(Dispatchers.Default) {
        try {
            val importName = importElement.name

            // 检查是否为库导入
            val isLibrary = config.libraryPrefixes.any { prefix ->
                importName.startsWith(prefix)
            }

            // 检查是否为标准库导入
            val isStandardLibrary = config.standardLibraryPrefixes.any { prefix ->
                importName.startsWith(prefix)
            }

            // 查找目标元素
            val targetElement = findTargetElement(importName, allElements)

            // 如果是库导入且启用了库分析，获取库信息
            val libraryInfo = if (isLibrary && config.enableLibraryAnalysis) {
                getLibraryInfo(importName)
            } else {
                null
            }

            return@withContext ImportAnalysisResult(
                importElement = importElement,
                targetElement = targetElement,
                isLibrary = isLibrary,
                isStandardLibrary = isStandardLibrary,
                libraryInfo = libraryInfo
            )
        } catch (e: Exception) {
            logger.error(e) { "分析导入语句时出错: ${importElement.name}, ${e.message}" }
            return@withContext ImportAnalysisResult(importElement)
        }
    }

    /**
     * 查找目标元素
     *
     * @param importName 导入名称
     * @param allElements 所有代码元素
     * @return 目标元素，如果不存在则返回null
     */
    private fun findTargetElement(importName: String, allElements: List<CodeElement>): CodeElement? {
        // 处理通配符导入
        if (importName.endsWith(".*")) {
            return null
        }

        // 查找完全匹配的元素
        return allElements.find { element ->
            element.qualifiedName == importName
        }
    }

    /**
     * 获取库信息
     *
     * @param importName 导入名称
     * @return 库信息
     */
    private suspend fun getLibraryInfo(importName: String): LibraryInfo? = withContext(Dispatchers.IO) {
        try {
            // 提取库名称
            val libraryName = extractLibraryName(importName)

            // 检查缓存
            libraryInfoCache[libraryName]?.let {
                return@withContext it
            }

            // 创建基本库信息
            val libraryInfo = LibraryInfo(
                name = libraryName
            )

            // 缓存库信息
            libraryInfoCache[libraryName] = libraryInfo

            return@withContext libraryInfo
        } catch (e: Exception) {
            logger.error(e) { "获取库信息时出错: $importName, ${e.message}" }
            return@withContext null
        }
    }

    /**
     * 提取库名称
     *
     * @param importName 导入名称
     * @return 库名称
     */
    private fun extractLibraryName(importName: String): String {
        // 简单实现：取前两个包名
        val parts = importName.split('.')
        return if (parts.size >= 2) {
            "${parts[0]}.${parts[1]}"
        } else {
            importName
        }
    }

    /**
     * 生成导入记忆
     *
     * @param analysisResults 导入分析结果列表
     * @return 语义记忆列表
     */
    fun generateImportMemories(analysisResults: List<ImportAnalysisResult>): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 按文件分组
        val resultsByFile = analysisResults.groupBy { it.importElement.parent?.id }

        // 为每个文件生成记忆
        resultsByFile.forEach { (fileId, results) ->
            if (fileId == null) return@forEach

            val fileElement = results.firstOrNull()?.importElement?.parent ?: return@forEach

            // 标准库导入
            val standardLibraryImports = results.filter { it.isStandardLibrary }
            if (standardLibraryImports.isNotEmpty()) {
                val content = buildString {
                    append("文件 ${fileElement.name} 导入了以下标准库:")
                    standardLibraryImports.forEach { result ->
                        append("\n- ${result.importElement.name}")
                    }
                }

                memories.add(
                    SemanticMemory(
                        type = MemoryType.IMPORT_STATEMENT,
                        content = content,
                        sourceElements = listOf(fileElement) + standardLibraryImports.map { it.importElement },
                        importance = ImportanceLevel.MEDIUM,
                        metadata = mutableMapOf(
                            "fileId" to fileId,
                            "importCount" to standardLibraryImports.size,
                            "isStandardLibrary" to true
                        )
                    )
                )
            }

            // 第三方库导入
            val libraryImports = results.filter { it.isLibrary && !it.isStandardLibrary }
            if (libraryImports.isNotEmpty()) {
                val content = buildString {
                    append("文件 ${fileElement.name} 导入了以下第三方库:")
                    libraryImports.forEach { result ->
                        append("\n- ${result.importElement.name}")
                    }
                }

                memories.add(
                    SemanticMemory(
                        type = MemoryType.IMPORT_STATEMENT,
                        content = content,
                        sourceElements = listOf(fileElement) + libraryImports.map { it.importElement },
                        importance = ImportanceLevel.HIGH,
                        metadata = mutableMapOf(
                            "fileId" to fileId,
                            "importCount" to libraryImports.size,
                            "isLibrary" to true
                        )
                    )
                )
            }

            // 项目内导入
            val projectImports = results.filter { !it.isLibrary && !it.isStandardLibrary && it.targetElement != null }
            if (projectImports.isNotEmpty()) {
                val content = buildString {
                    append("文件 ${fileElement.name} 导入了以下项目内类型:")
                    projectImports.forEach { result ->
                        val targetName = result.targetElement?.name ?: "未知"
                        val targetType = result.targetElement?.type?.name?.lowercase() ?: "未知"
                        append("\n- ${result.importElement.name} ($targetType: $targetName)")
                    }
                }

                memories.add(
                    SemanticMemory(
                        type = MemoryType.IMPORT_STATEMENT,
                        content = content,
                        sourceElements = listOf(fileElement) + projectImports.flatMap { 
                            listOfNotNull(it.importElement, it.targetElement) 
                        },
                        importance = ImportanceLevel.HIGH,
                        metadata = mutableMapOf(
                            "fileId" to fileId,
                            "importCount" to projectImports.size,
                            "isProjectImport" to true
                        )
                    )
                )
            }

            // 导入分析记忆
            val content = buildString {
                append("文件 ${fileElement.name} 的导入分析:")
                append("\n- 总导入数: ${results.size}")
                append("\n- 标准库导入: ${standardLibraryImports.size}")
                append("\n- 第三方库导入: ${libraryImports.size}")
                append("\n- 项目内导入: ${projectImports.size}")
                
                // 添加主要依赖信息
                val mainLibraries = libraryImports
                    .mapNotNull { it.libraryInfo?.name }
                    .distinct()
                    .take(5)
                
                if (mainLibraries.isNotEmpty()) {
                    append("\n\n主要依赖库:")
                    mainLibraries.forEach { library ->
                        append("\n- $library")
                    }
                }
            }

            memories.add(
                SemanticMemory(
                    type = MemoryType.IMPORT_ANALYSIS,
                    content = content,
                    sourceElements = listOf(fileElement) + results.map { it.importElement },
                    importance = ImportanceLevel.MEDIUM,
                    metadata = mutableMapOf(
                        "fileId" to fileId,
                        "totalImports" to results.size,
                        "standardLibraryImports" to standardLibraryImports.size,
                        "libraryImports" to libraryImports.size,
                        "projectImports" to projectImports.size
                    )
                )
            )
        }

        return memories
    }

    /**
     * 生成库API记忆
     *
     * @param analysisResults 导入分析结果列表
     * @return 语义记忆列表
     */
    fun generateLibraryApiMemories(analysisResults: List<ImportAnalysisResult>): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 按库分组
        val resultsByLibrary = analysisResults
            .filter { it.isLibrary && it.libraryInfo != null }
            .groupBy { it.libraryInfo?.name }

        // 为每个库生成记忆
        resultsByLibrary.forEach { (libraryName, results) ->
            if (libraryName == null) return@forEach

            val libraryInfo = results.firstOrNull()?.libraryInfo ?: return@forEach

            val content = buildString {
                append("库 $libraryName 的API使用分析:")
                append("\n- 导入次数: ${results.size}")
                
                // 按文件分组
                val fileGroups = results.groupBy { it.importElement.parent?.name }
                append("\n- 使用该库的文件数: ${fileGroups.size}")
                
                // 列出前5个使用该库的文件
                val topFiles = fileGroups.keys.take(5)
                if (topFiles.isNotEmpty()) {
                    append("\n\n主要使用文件:")
                    topFiles.forEach { fileName ->
                        if (fileName != null) {
                            append("\n- $fileName")
                        }
                    }
                }
            }

            memories.add(
                SemanticMemory(
                    type = MemoryType.LIBRARY_API,
                    content = content,
                    sourceElements = results.map { it.importElement },
                    importance = ImportanceLevel.MEDIUM,
                    metadata = mutableMapOf(
                        "libraryName" to libraryName,
                        "importCount" to results.size,
                        "fileCount" to results.groupBy { it.importElement.parent?.id }.size
                    )
                )
            )
        }

        return memories
    }

    /**
     * 分析符号节点的库使用
     *
     * @param node 符号节点
     * @param importResults 导入分析结果列表
     * @return 语义记忆列表
     */
    fun analyzeSymbolLibraryUsage(node: SymbolNode, importResults: List<ImportAnalysisResult>): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        // 只为类和接口生成库使用记忆
        if (node.type !in setOf(SymbolType.CLASS, SymbolType.INTERFACE)) {
            return memories
        }

        // 查找与该符号相关的导入
        val relatedImports = importResults.filter { result ->
            result.importElement.parent?.id == node.id
        }

        if (relatedImports.isEmpty()) {
            return memories
        }

        // 按库类型分组
        val standardLibraryImports = relatedImports.filter { it.isStandardLibrary }
        val thirdPartyLibraryImports = relatedImports.filter { it.isLibrary && !it.isStandardLibrary }

        // 生成标准库使用记忆
        if (standardLibraryImports.isNotEmpty()) {
            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 使用了以下标准库:")
                
                // 按库分组
                val libraryGroups = standardLibraryImports.groupBy { 
                    it.importElement.name.split('.').take(2).joinToString(".")
                }
                
                libraryGroups.entries.take(10).forEach { (library, imports) ->
                    append("\n- $library (${imports.size} 个导入)")
                }
                
                if (libraryGroups.size > 10) {
                    append("\n- ... 等 ${libraryGroups.size - 10} 个标准库")
                }
            }

            memories.add(
                SemanticMemory(
                    type = MemoryType.LIBRARY_USAGE,
                    content = content,
                    sourceSymbols = listOf(node),
                    sourceElements = standardLibraryImports.map { it.importElement },
                    importance = ImportanceLevel.MEDIUM,
                    metadata = mutableMapOf(
                        "symbolId" to node.id,
                        "symbolType" to node.type.name,
                        "importCount" to standardLibraryImports.size,
                        "isStandardLibrary" to true
                    )
                )
            )
        }

        // 生成第三方库使用记忆
        if (thirdPartyLibraryImports.isNotEmpty()) {
            val content = buildString {
                append("${node.type.name.lowercase()} ${node.name} 使用了以下第三方库:")
                
                // 按库分组
                val libraryGroups = thirdPartyLibraryImports.groupBy { 
                    it.libraryInfo?.name ?: it.importElement.name.split('.').take(2).joinToString(".")
                }
                
                libraryGroups.entries.take(10).forEach { (library, imports) ->
                    append("\n- $library (${imports.size} 个导入)")
                }
                
                if (libraryGroups.size > 10) {
                    append("\n- ... 等 ${libraryGroups.size - 10} 个第三方库")
                }
            }

            memories.add(
                SemanticMemory(
                    type = MemoryType.LIBRARY_USAGE,
                    content = content,
                    sourceSymbols = listOf(node),
                    sourceElements = thirdPartyLibraryImports.map { it.importElement },
                    importance = ImportanceLevel.HIGH,
                    metadata = mutableMapOf(
                        "symbolId" to node.id,
                        "symbolType" to node.type.name,
                        "importCount" to thirdPartyLibraryImports.size,
                        "isThirdPartyLibrary" to true
                    )
                )
            )
        }

        return memories
    }
}
