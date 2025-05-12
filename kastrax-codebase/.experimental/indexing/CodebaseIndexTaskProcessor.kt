package ai.kastrax.codebase.indexing

import ai.kastrax.codebase.semantic.graph.SymbolGraph
import ai.kastrax.codebase.semantic.graph.SymbolRelationType
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.parser.CodeParserFactory
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 代码库索引任务处理器
 *
 * 处理代码库索引任务，包括解析代码、构建符号图、生成嵌入向量等
 *
 * @property symbolGraph 符号图
 * @property embeddingService 嵌入服务
 * @property embeddingCache 嵌入缓存
 */
class CodebaseIndexTaskProcessor(
    private val symbolGraph: SymbolGraph,
    private val embeddingService: EmbeddingService,
    private val embeddingCache: ConcurrentHashMap<String, List<Float>> = ConcurrentHashMap()
) : IndexTaskProcessor {

    /**
     * 处理索引任务
     *
     * @param task 索引任务
     * @return 处理结果
     */
    override suspend fun process(task: IndexTask): IndexTaskResult {
        return when (task) {
            is AddFileTask -> processAddFileTask(task)
            is UpdateFileTask -> processUpdateFileTask(task)
            is DeleteFileTask -> processDeleteFileTask(task)
            else -> IndexTaskResult(
                success = false,
                message = "不支持的任务类型: ${task.javaClass.simpleName}"
            )
        }
    }

    /**
     * 处理添加文件任务
     *
     * @param task 添加文件任务
     * @return 处理结果
     */
    private suspend fun processAddFileTask(task: AddFileTask): IndexTaskResult {
        val filePath = task.filePath
        val content = task.content

        try {
            // 解析代码文件
            val fileElement = parseFile(filePath, content)
            if (fileElement == null) {
                return IndexTaskResult(
                    success = false,
                    message = "无法解析文件: $filePath"
                )
            }

            // 添加符号到图中
            addSymbolsToGraph(fileElement)

            // 构建符号关系
            buildSymbolRelations(fileElement)

            // 生成嵌入向量
            generateEmbeddings(fileElement)

            return IndexTaskResult(
                success = true,
                message = "成功添加文件: $filePath",
                metadata = mapOf(
                    "filePath" to filePath.toString(),
                    "symbolCount" to fileElement.getAllChildren().size + 1
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "处理添加文件任务时出错: $filePath" }
            return IndexTaskResult(
                success = false,
                message = "处理添加文件任务时出错: $filePath - ${e.message}"
            )
        }
    }

    /**
     * 处理更新文件任务
     *
     * @param task 更新文件任务
     * @return 处理结果
     */
    private suspend fun processUpdateFileTask(task: UpdateFileTask): IndexTaskResult {
        val filePath = task.filePath
        val content = task.content

        try {
            // 删除旧的符号
            symbolGraph.removeFileSymbols(filePath)

            // 解析代码文件
            val fileElement = parseFile(filePath, content)
            if (fileElement == null) {
                return IndexTaskResult(
                    success = false,
                    message = "无法解析文件: $filePath"
                )
            }

            // 添加符号到图中
            addSymbolsToGraph(fileElement)

            // 构建符号关系
            buildSymbolRelations(fileElement)

            // 生成嵌入向量
            generateEmbeddings(fileElement)

            return IndexTaskResult(
                success = true,
                message = "成功更新文件: $filePath",
                metadata = mapOf(
                    "filePath" to filePath.toString(),
                    "symbolCount" to fileElement.getAllChildren().size + 1
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "处理更新文件任务时出错: $filePath" }
            return IndexTaskResult(
                success = false,
                message = "处理更新文件任务时出错: $filePath - ${e.message}"
            )
        }
    }

    /**
     * 处理删除文件任务
     *
     * @param task 删除文件任务
     * @return 处理结果
     */
    private suspend fun processDeleteFileTask(task: DeleteFileTask): IndexTaskResult {
        val filePath = task.filePath

        try {
            // 删除符号
            val count = symbolGraph.removeFileSymbols(filePath)

            return IndexTaskResult(
                success = true,
                message = "成功删除文件: $filePath",
                metadata = mapOf(
                    "filePath" to filePath.toString(),
                    "symbolCount" to count
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "处理删除文件任务时出错: $filePath" }
            return IndexTaskResult(
                success = false,
                message = "处理删除文件任务时出错: $filePath - ${e.message}"
            )
        }
    }

    /**
     * 解析文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 代码元素
     */
    private fun parseFile(filePath: Path, content: String): CodeElement? {
        try {
            return CodeParserFactory.parseFile(filePath, content)
        } catch (e: Exception) {
            logger.error(e) { "解析文件时出错: $filePath" }
            return null
        }
    }

    /**
     * 添加符号到图中
     *
     * @param element 代码元素
     */
    private fun addSymbolsToGraph(element: CodeElement) {
        // 添加当前元素
        symbolGraph.addSymbol(element)

        // 递归添加子元素
        element.children.forEach { child ->
            addSymbolsToGraph(child)
        }
    }

    /**
     * 构建符号关系
     *
     * @param element 代码元素
     */
    private fun buildSymbolRelations(element: CodeElement) {
        // 处理父子关系
        element.children.forEach { child ->
            symbolGraph.addRelation(
                element,
                child,
                SymbolRelationType.CONTAINS
            )
            buildSymbolRelations(child)
        }

        // 处理类型特定的关系
        when (element.type) {
            CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> {
                processTypeRelations(element)
            }
            CodeElementType.METHOD, CodeElementType.CONSTRUCTOR -> {
                processMethodRelations(element)
            }
            CodeElementType.FIELD, CodeElementType.PROPERTY -> {
                processFieldRelations(element)
            }
            CodeElementType.IMPORT -> {
                processImportRelations(element)
            }
            else -> {
                // 其他类型暂不处理
            }
        }
    }

    /**
     * 处理类型关系
     *
     * @param element 代码元素
     */
    private fun processTypeRelations(element: CodeElement) {
        // 处理继承关系
        val extends = element.metadata["extends"] as? List<*>
        extends?.forEach { ext ->
            val extName = ext.toString()
            // 查找目标类型
            findSymbolByQualifiedName(extName)?.let { target ->
                symbolGraph.addRelation(
                    element,
                    target,
                    SymbolRelationType.EXTENDS
                )
            }
        }

        // 处理实现关系
        val implements = element.metadata["implements"] as? List<*>
        implements?.forEach { impl ->
            val implName = impl.toString()
            // 查找目标接口
            findSymbolByQualifiedName(implName)?.let { target ->
                symbolGraph.addRelation(
                    element,
                    target,
                    SymbolRelationType.IMPLEMENTS
                )
            }
        }
    }

    /**
     * 处理方法关系
     *
     * @param element 代码元素
     */
    private fun processMethodRelations(element: CodeElement) {
        // 处理重写关系
        if (element.modifiers.contains(ai.kastrax.codebase.semantic.model.Modifier.OVERRIDE)) {
            // 查找父类中的同名方法
            val parent = element.parent
            if (parent != null) {
                val parentType = parent.metadata["extends"] as? List<*>
                parentType?.forEach { ext ->
                    val extName = ext.toString()
                    // 查找父类
                    findSymbolByQualifiedName(extName)?.let { parentClass ->
                        // 查找父类中的同名方法
                        parentClass.findChildren { it.name == element.name && it.type == element.type }
                            .forEach { overriddenMethod ->
                                symbolGraph.addRelation(
                                    element,
                                    overriddenMethod,
                                    SymbolRelationType.OVERRIDES
                                )
                            }
                    }
                }
            }
        }

        // 处理参数类型关系
        val parameters = element.metadata["parameters"] as? List<*>
        parameters?.forEach { param ->
            val paramType = param.toString()
            // 查找参数类型
            findSymbolByQualifiedName(paramType)?.let { target ->
                symbolGraph.addRelation(
                    element,
                    target,
                    SymbolRelationType.USES
                )
            }
        }

        // 处理返回类型关系
        val returnType = element.metadata["returnType"] as? String
        if (!returnType.isNullOrEmpty() && returnType != "void") {
            // 查找返回类型
            findSymbolByQualifiedName(returnType)?.let { target ->
                symbolGraph.addRelation(
                    element,
                    target,
                    SymbolRelationType.USES
                )
            }
        }
    }

    /**
     * 处理字段关系
     *
     * @param element 代码元素
     */
    private fun processFieldRelations(element: CodeElement) {
        // 处理字段类型关系
        val fieldType = element.metadata["type"] as? String
        if (!fieldType.isNullOrEmpty()) {
            // 查找字段类型
            findSymbolByQualifiedName(fieldType)?.let { target ->
                symbolGraph.addRelation(
                    element,
                    target,
                    SymbolRelationType.USES
                )
            }
        }
    }

    /**
     * 处理导入关系
     *
     * @param element 代码元素
     */
    private fun processImportRelations(element: CodeElement) {
        val importName = element.name
        // 查找导入的类型
        findSymbolByQualifiedName(importName)?.let { target ->
            symbolGraph.addRelation(
                element,
                target,
                SymbolRelationType.IMPORTS
            )
        }
    }

    /**
     * 根据限定名查找符号
     *
     * @param qualifiedName 限定名
     * @return 代码元素
     */
    private fun findSymbolByQualifiedName(qualifiedName: String): CodeElement? {
        return symbolGraph.findSymbols { it.qualifiedName == qualifiedName }.firstOrNull()
    }

    /**
     * 生成嵌入向量
     *
     * @param element 代码元素
     */
    private suspend fun generateEmbeddings(element: CodeElement) {
        try {
            // 生成当前元素的嵌入向量
            val text = getElementText(element)
            if (text.isNotEmpty()) {
                val embedding = embeddingService.embed(text).toList()
                embeddingCache[element.id] = embedding
            }

            // 递归生成子元素的嵌入向量
            element.children.forEach { child ->
                generateEmbeddings(child)
            }
        } catch (e: Exception) {
            logger.error(e) { "生成嵌入向量时出错: ${element.id}" }
        }
    }

    /**
     * 获取元素的文本表示
     *
     * @param element 代码元素
     * @return 文本表示
     */
    private fun getElementText(element: CodeElement): String {
        val sb = StringBuilder()

        // 添加元素类型和名称
        sb.append("${element.type.name.lowercase()}: ${element.name}\n")

        // 添加限定名
        sb.append("qualified name: ${element.qualifiedName}\n")

        // 添加可见性和修饰符
        if (element.visibility != ai.kastrax.codebase.semantic.model.Visibility.UNKNOWN) {
            sb.append("visibility: ${element.visibility.name.lowercase()}\n")
        }
        if (element.modifiers.isNotEmpty()) {
            sb.append("modifiers: ${element.modifiers.joinToString(", ") { it.name.lowercase() }}\n")
        }

        // 添加文档注释
        if (element.documentation.isNotEmpty()) {
            sb.append("documentation: ${element.documentation}\n")
        }

        // 添加类型特定的信息
        when (element.type) {
            CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> {
                // 添加继承和实现信息
                val extends = element.metadata["extends"] as? List<*>
                if (extends?.isNotEmpty() == true) {
                    sb.append("extends: ${extends.joinToString(", ")}\n")
                }
                val implements = element.metadata["implements"] as? List<*>
                if (implements?.isNotEmpty() == true) {
                    sb.append("implements: ${implements.joinToString(", ")}\n")
                }
            }
            CodeElementType.METHOD, CodeElementType.CONSTRUCTOR -> {
                // 添加参数和返回类型信息
                val parameters = element.metadata["parameters"] as? List<*>
                if (parameters?.isNotEmpty() == true) {
                    sb.append("parameters: ${parameters.joinToString(", ")}\n")
                }
                val returnType = element.metadata["returnType"] as? String
                if (!returnType.isNullOrEmpty()) {
                    sb.append("return type: $returnType\n")
                }
            }
            CodeElementType.FIELD, CodeElementType.PROPERTY -> {
                // 添加字段类型信息
                val fieldType = element.metadata["type"] as? String
                if (!fieldType.isNullOrEmpty()) {
                    sb.append("type: $fieldType\n")
                }
                val initializer = element.metadata["initializer"] as? String
                if (!initializer.isNullOrEmpty()) {
                    sb.append("initializer: $initializer\n")
                }
            }
            else -> {
                // 其他类型暂不处理
            }
        }

        return sb.toString()
    }

    /**
     * 获取嵌入向量
     *
     * @param id 符号ID
     * @return 嵌入向量
     */
    fun getEmbedding(id: String): List<Float>? {
        return embeddingCache[id]
    }

    /**
     * 获取所有嵌入向量
     *
     * @return 嵌入向量映射表
     */
    fun getAllEmbeddings(): Map<String, List<Float>> {
        return embeddingCache.toMap()
    }

    /**
     * 清空嵌入缓存
     */
    fun clearEmbeddingCache() {
        embeddingCache.clear()
    }
}
