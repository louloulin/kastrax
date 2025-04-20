package ai.kastrax.rag.embedding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * FastEmbed 嵌入服务，使用 Python 的 FastEmbed 库生成文本的嵌入向量。
 *
 * 注意：这个实现需要安装 Python 和 FastEmbed 库。
 * 可以使用以下命令安装：
 * ```
 * pip install fastembed
 * ```
 *
 * @property modelName 模型名称，默认为 "BAAI/bge-small-zh-v1.5"
 * @property dimensions 嵌入向量的维度，默认为 384
 * @property maxLength 最大文本长度，默认为 512
 * @property normalize 是否归一化嵌入向量，默认为 true
 * @property pythonCommand Python 命令，默认为 "python"
 * @property timeout 超时时间（秒），默认为 30
 */
class FastEmbedEmbeddingService(
    private val modelName: String = "BAAI/bge-small-zh-v1.5",
    private val dimensions: Int = 384,
    private val maxLength: Int = 512,
    private val normalize: Boolean = true,
    private val pythonCommand: String = "python",
    private val timeout: Long = 30
) : EmbeddingService, Closeable {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val scriptFile: File

    init {
        logger.info { "Initializing FastEmbed with model: $modelName" }

        // 创建临时 Python 脚本文件
        scriptFile = createPythonScript()

        // 验证 Python 和 FastEmbed 库
        validatePythonAndFastEmbed()

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
     * 关闭服务，删除临时脚本文件。
     */
    override fun close() {
        try {
            if (scriptFile.exists()) {
                scriptFile.delete()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting temporary script file" }
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

    # 初始化嵌入模型
    embedding_model = TextEmbedding(model_name=model_name, max_length=max_length)

    # 生成嵌入
    embeddings = list(embedding_model.embed(texts, normalize=normalize))

    # 将嵌入转换为列表
    embeddings_list = [emb.tolist() for emb in embeddings]

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
     * 验证 Python 和 FastEmbed 库是否可用。
     */
    private fun validatePythonAndFastEmbed() {
        try {
            // 验证 Python
            val pythonProcess = ProcessBuilder(pythonCommand, "--version")
                .redirectErrorStream(true)
                .start()

            val pythonResult = pythonProcess.waitFor(timeout, TimeUnit.SECONDS)
            if (!pythonResult) {
                throw RuntimeException("Python command timed out")
            }

            // 验证 FastEmbed
            val fastembedProcess = ProcessBuilder(pythonCommand, "-c", "import fastembed; print('FastEmbed available')")
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
     * 使用 Python 脚本生成文本嵌入。
     */
    private fun embedTexts(texts: List<String>): List<Embedding> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        try {
            // 创建临时输入和输出文件
            val tempDir = Files.createTempDirectory("kastrax-fastembed-io").toFile()
            val inputFile = File(tempDir, "input.json")
            val outputFile = File(tempDir, "output.json")

            // 写入输入文件
            objectMapper.writeValue(inputFile, texts)

            // 执行 Python 脚本
            val process = ProcessBuilder(
                pythonCommand,
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
