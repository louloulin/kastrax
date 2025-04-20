package ai.kastrax.rag.embedding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * FastEmbed 嵌入服务，使用 GraalPy 和 FastEmbed 库生成文本的嵌入向量。
 *
 * 注意：这个实现使用 GraalVM 的 Python 支持（GraalPy）和 FastEmbed 库。
 * 需要在项目中添加以下依赖：
 * ```
 * implementation("org.graalvm.polyglot:polyglot:23.0.0")
 * implementation("org.graalvm.polyglot:python:23.0.0")
 * ```
 *
 * 并且需要安装 FastEmbed 库：
 * ```
 * graalpy -m pip install fastembed
 * ```
 *
 * @property modelName 模型名称，默认为 "BAAI/bge-small-zh-v1.5"
 * @property dimensions 嵌入向量的维度，默认为 384
 * @property maxLength 最大文本长度，默认为 512
 * @property normalize 是否归一化嵌入向量，默认为 true
 * @property graalPyPath GraalPy 路径，默认为 "graalpy"（需要在 PATH 中）
 * @property timeout 超时时间（秒），默认为 30
 * @property usePolyglot 是否使用 Polyglot API，默认为 true（如果为 false，则使用命令行方式）
 */
class FastEmbedEmbeddingService(
    private val modelName: String = "BAAI/bge-small-zh-v1.5",
    private val dimensions: Int = 384,
    private val maxLength: Int = 512,
    private val normalize: Boolean = true,
    private val graalPyPath: String = "graalpy",
    private val timeout: Long = 30,
    private val usePolyglot: Boolean = true
) : EmbeddingService, Closeable {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val scriptFile: File
    private var polyglotContext: Context? = null
    private var pythonModule: Value? = null

    init {
        logger.info { "Initializing FastEmbed with model: $modelName" }

        // 创建临时 Python 脚本文件
        scriptFile = createPythonScript()

        if (usePolyglot) {
            try {
                // 尝试初始化 GraalPy Polyglot 上下文
                initializePolyglotContext()
                logger.info { "Successfully initialized GraalPy Polyglot context" }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to initialize GraalPy Polyglot context, falling back to command line mode" }
                // 验证 Python 和 FastEmbed 库
                validateGraalPyAndFastEmbed()
            }
        } else {
            // 验证 Python 和 FastEmbed 库
            validateGraalPyAndFastEmbed()
        }

        logger.info { "FastEmbed initialized with embedding dimensions: $dimensions" }
    }

    override suspend fun embed(text: String): Embedding {
        return withContext(Dispatchers.IO) {
            try {
                val embeddings = embedTexts(listOf(text))
                embeddings.first()
            } catch (e: Exception) {
                logger.error(e) { "Error generating embedding for text" }
                // 返回零向量作为后备
                Embedding(List(dimensions) { 0f })
            }
        }
    }

    override suspend fun embedBatch(texts: List<String>): List<Embedding> {
        return withContext(Dispatchers.IO) {
            try {
                embedTexts(texts)
            } catch (e: Exception) {
                logger.error(e) { "Error generating batch embeddings" }
                // 返回零向量作为后备
                texts.map { Embedding(List(dimensions) { 0f }) }
            }
        }
    }

    /**
     * 关闭服务，关闭 Polyglot 上下文并删除临时脚本文件。
     */
    override fun close() {
        try {
            // 关闭 Polyglot 上下文
            polyglotContext?.close()

            // 删除临时脚本文件
            if (scriptFile.exists()) {
                scriptFile.delete()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error closing FastEmbedEmbeddingService" }
        }
    }

    /**
     * 创建 Python 脚本文件。
     */
    private fun createPythonScript(): File {
        val scriptContent = """
import sys
import json
from fastembed import TextEmbedding

def embed_texts(texts, model_name, max_length, normalize):
    # 初始化嵌入模型
    embedding_model = TextEmbedding(model_name=model_name, max_length=max_length)

    # 生成嵌入
    embeddings = list(embedding_model.embed(texts, normalize=normalize))

    # 将嵌入转换为列表
    return [emb.tolist() for emb in embeddings]

# 命令行模式下的主函数
def main():
    # 解析命令行参数
    input_file = sys.argv[1]
    output_file = sys.argv[2]
    model_name = sys.argv[3]
    max_length = int(sys.argv[4])
    normalize = sys.argv[5].lower() == 'true'

    # 读取输入文本
    with open(input_file, 'r', encoding='utf-8') as f:
        texts = json.load(f)

    # 生成嵌入
    embeddings_list = embed_texts(texts, model_name, max_length, normalize)

    # 保存嵌入到输出文件
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(embeddings_list, f)

if __name__ == '__main__':
    main()
        """.trimIndent()

        val tempDir = Files.createTempDirectory("kastrax-fastembed").toFile()
        val scriptFile = File(tempDir, "fastembed_script.py")
        scriptFile.writeText(scriptContent)
        scriptFile.deleteOnExit()
        tempDir.deleteOnExit()

        return scriptFile
    }

    /**
     * 初始化 GraalPy Polyglot 上下文。
     */
    private fun initializePolyglotContext() {
        try {
            logger.info { "Initializing GraalPy Polyglot context" }

            // 创建 Polyglot 上下文
            polyglotContext = Context.newBuilder()
                .allowAllAccess(true)
                .build()

            // 加载 Python 脚本
            val source = Source.newBuilder("python", scriptFile).build()
            polyglotContext?.eval(source)

            // 获取 Python 模块
            pythonModule = polyglotContext?.getBindings("python")

            // 验证 FastEmbed 库
            val result = polyglotContext?.eval("python", "import fastembed; print('FastEmbed available'); 'Success'")
            if (result?.asString() != "Success") {
                throw RuntimeException("Failed to import fastembed in GraalPy")
            }

            logger.info { "GraalPy Polyglot context initialized successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Error initializing GraalPy Polyglot context" }
            throw RuntimeException("Failed to initialize GraalPy Polyglot context", e)
        }
    }

    /**
     * 验证 Python 和 FastEmbed 库是否可用。
     */
    private fun validateGraalPyAndFastEmbed() {
        try {
            // 验证 Python
            logger.info { "Validating Python with command: $graalPyPath" }
            val pythonProcess = ProcessBuilder(graalPyPath, "--version")
                .redirectErrorStream(true)
                .start()

            val pythonResult = pythonProcess.waitFor(timeout, TimeUnit.SECONDS)
            if (!pythonResult) {
                throw RuntimeException("Python command timed out")
            }

            // 读取输出
            val pythonOutput = BufferedReader(InputStreamReader(pythonProcess.inputStream)).use { it.readText() }
            logger.info { "Python version: $pythonOutput" }

            // 验证 FastEmbed
            logger.info { "Validating FastEmbed library" }
            val fastembedProcess = ProcessBuilder(graalPyPath, "-c", "import fastembed; print('FastEmbed available')")
                .redirectErrorStream(true)
                .start()

            val fastembedResult = fastembedProcess.waitFor(timeout, TimeUnit.SECONDS)
            if (!fastembedResult) {
                throw RuntimeException("FastEmbed import timed out")
            }

            val fastembedOutput = BufferedReader(InputStreamReader(fastembedProcess.inputStream)).readText()
            if (!fastembedOutput.contains("FastEmbed available")) {
                throw RuntimeException("FastEmbed not available: $fastembedOutput")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to validate Python and FastEmbed: ${e.message}")
        }
    }

    /**
     * 生成文本嵌入。
     */
    private fun embedTexts(texts: List<String>): List<Embedding> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        return if (usePolyglot && polyglotContext != null) {
            try {
                embedTextsWithPolyglot(texts)
            } catch (e: Exception) {
                logger.warn(e) { "Error using Polyglot API for embedding, falling back to command line" }
                embedTextsWithCommandLine(texts)
            }
        } else {
            embedTextsWithCommandLine(texts)
        }
    }

    /**
     * 使用 GraalPy Polyglot API 生成文本嵌入。
     */
    private fun embedTextsWithPolyglot(texts: List<String>): List<Embedding> {
        try {
            logger.info { "Generating embeddings for ${texts.size} texts using GraalPy Polyglot API" }

            // 调用 Python 函数
            val pyTexts = polyglotContext?.eval("python", "texts = ${objectMapper.writeValueAsString(texts)}; texts")
            val embedFunction = polyglotContext?.eval("python", "embed_texts")

            val result = embedFunction?.execute(
                pyTexts,
                modelName,
                maxLength,
                normalize
            )

            // 转换结果
            val embeddings = mutableListOf<Embedding>()
            val size = result?.getArraySize() ?: 0

            for (i in 0 until size) {
                val embedding = result?.getArrayElement(i)
                val embeddingSize = embedding?.getArraySize() ?: 0
                val vector = mutableListOf<Float>()

                for (j in 0 until embeddingSize) {
                    val value = embedding?.getArrayElement(j)?.asFloat() ?: 0f
                    vector.add(value)
                }

                embeddings.add(Embedding(vector))
            }

            return embeddings
        } catch (e: Exception) {
            logger.error(e) { "Error generating embeddings with GraalPy Polyglot API" }
            throw e
        }
    }

    /**
     * 使用命令行方式生成文本嵌入。
     */
    private fun embedTextsWithCommandLine(texts: List<String>): List<Embedding> {
        try {
            // 创建临时输入和输出文件
            val tempDir = Files.createTempDirectory("kastrax-fastembed-io").toFile()
            val inputFile = File(tempDir, "input.json")
            val outputFile = File(tempDir, "output.json")

            // 写入输入文件
            objectMapper.writeValue(inputFile, texts)

            // 执行 Python 脚本
            logger.info { "Executing Python script with command: $graalPyPath" }
            val process = ProcessBuilder(
                graalPyPath,
                scriptFile.absolutePath,
                inputFile.absolutePath,
                outputFile.absolutePath,
                modelName,
                maxLength.toString(),
                normalize.toString()
            )
                .redirectErrorStream(true)
                .start()

            // 等待进程完成
            val result = process.waitFor(timeout, TimeUnit.SECONDS)
            if (!result) {
                process.destroyForcibly()
                throw RuntimeException("Process timed out after $timeout seconds")
            }

            // 检查进程退出码
            if (process.exitValue() != 0) {
                val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                throw RuntimeException("Process exited with code ${process.exitValue()}: $output")
            }

            // 读取输出文件
            val embeddings: List<List<Float>> = objectMapper.readValue(outputFile)

            // 清理临时文件
            inputFile.delete()
            outputFile.delete()
            tempDir.delete()

            // 转换为 Embedding 对象
            return embeddings.map { Embedding(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error executing Python script" }
            throw e
        }
    }
}
