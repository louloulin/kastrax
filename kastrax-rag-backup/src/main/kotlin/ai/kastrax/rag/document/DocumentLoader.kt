package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URL
import org.jsoup.Jsoup
import org.jsoup.nodes.Document as JsoupDocument

private val logger = KotlinLogging.logger {}

/**
 * 文档加载器接口，用于从各种来源加载文档。
 */
interface DocumentLoader {
    /**
     * 加载文档。
     *
     * @return 加载的文档列表
     */
    suspend fun load(): List<Document>
}

/**
 * 文本文件文档加载器，从文本文件加载文档。
 *
 * @property file 要加载的文件
 * @property encoding 文件编码，默认为 UTF-8
 * @property metadata 要添加到文档的元数据
 */
class TextFileDocumentLoader(
    private val file: File,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap()
) : DocumentLoader {

    constructor(
        filePath: String,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap()
    ) : this(File(filePath), encoding, metadata)

    override suspend fun load(): List<Document> {
        logger.debug { "Loading text file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            val content = file.readText(charset(encoding))
            val baseMetadata = mapOf(
                "source" to file.absolutePath,
                "file_name" to file.name,
                "file_extension" to (file.extension.takeIf { it.isNotEmpty() } ?: "txt"),
                "file_size" to file.length(),
                "file_last_modified" to file.lastModified()
            )

            val fullMetadata = baseMetadata + metadata

            listOf(Document(content, fullMetadata))
        } catch (e: Exception) {
            logger.error(e) { "Error loading text file: ${file.absolutePath}" }
            emptyList()
        }
    }
}

/**
 * HTML 文件文档加载器，从 HTML 文件加载文档。
 *
 * @property file 要加载的 HTML 文件
 * @property encoding 文件编码，默认为 UTF-8
 * @property metadata 要添加到文档的元数据
 * @property extractTitle 是否提取标题，默认为 true
 */
class HtmlFileDocumentLoader(
    private val file: File,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap(),
    private val extractTitle: Boolean = true
) : DocumentLoader {

    constructor(
        filePath: String,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap(),
        extractTitle: Boolean = true
    ) : this(File(filePath), encoding, metadata, extractTitle)

    override suspend fun load(): List<Document> {
        logger.debug { "Loading HTML file: ${file.absolutePath}" }

        if (!file.exists()) {
            logger.error { "File does not exist: ${file.absolutePath}" }
            return emptyList()
        }

        if (!file.isFile) {
            logger.error { "Path is not a file: ${file.absolutePath}" }
            return emptyList()
        }

        return try {
            val jsoupDoc = Jsoup.parse(file, encoding)
            val content = jsoupDoc.body().text()

            val baseMetadata = mutableMapOf(
                "source" to file.absolutePath,
                "file_name" to file.name,
                "file_extension" to (file.extension.takeIf { it.isNotEmpty() } ?: "html"),
                "file_size" to file.length(),
                "file_last_modified" to file.lastModified()
            )

            if (extractTitle) {
                jsoupDoc.title()?.takeIf { it.isNotEmpty() }?.let {
                    baseMetadata["title"] = it
                }
            }

            val fullMetadata = baseMetadata + metadata

            listOf(Document(content, fullMetadata))
        } catch (e: Exception) {
            logger.error(e) { "Error loading HTML file: ${file.absolutePath}" }
            emptyList()
        }
    }
}

/**
 * 网页文档加载器，从 URL 加载网页文档。
 *
 * @property url 要加载的 URL
 * @property metadata 要添加到文档的元数据
 * @property extractTitle 是否提取标题，默认为 true
 * @property timeout 连接超时时间（毫秒），默认为 10000
 */
class WebPageDocumentLoader(
    private val url: URL,
    private val metadata: Map<String, Any> = emptyMap(),
    private val extractTitle: Boolean = true,
    private val timeout: Int = 10000
) : DocumentLoader {

    constructor(
        urlString: String,
        metadata: Map<String, Any> = emptyMap(),
        extractTitle: Boolean = true,
        timeout: Int = 10000
    ) : this(URL(urlString), metadata, extractTitle, timeout)

    override suspend fun load(): List<Document> {
        logger.debug { "Loading web page: $url" }

        return try {
            val jsoupDoc = Jsoup.connect(url.toString())
                .timeout(timeout)
                .get()

            val content = jsoupDoc.body().text()

            val baseMetadata = mutableMapOf<String, Any>(
                "source" to url.toString(),
                "url" to url.toString(),
                "domain" to (url.host ?: ""),
                "path" to (url.path ?: "")
            )

            if (extractTitle) {
                jsoupDoc.title()?.takeIf { it.isNotEmpty() }?.let {
                    baseMetadata["title"] = it
                }
            }

            val fullMetadata = baseMetadata + metadata

            listOf(Document(content, fullMetadata))
        } catch (e: Exception) {
            logger.error(e) { "Error loading web page: $url" }
            emptyList()
        }
    }
}

/**
 * 目录文档加载器，从目录中加载多个文档。
 *
 * @property directory 要加载的目录
 * @property recursive 是否递归加载子目录，默认为 true
 * @property fileExtensions 要加载的文件扩展名列表，默认为 null（加载所有文件）
 * @property encoding 文件编码，默认为 UTF-8
 * @property metadata 要添加到所有文档的元数据
 */
class DirectoryDocumentLoader(
    private val directory: File,
    private val recursive: Boolean = true,
    private val fileExtensions: List<String>? = null,
    private val encoding: String = "UTF-8",
    private val metadata: Map<String, Any> = emptyMap()
) : DocumentLoader {

    constructor(
        directoryPath: String,
        recursive: Boolean = true,
        fileExtensions: List<String>? = null,
        encoding: String = "UTF-8",
        metadata: Map<String, Any> = emptyMap()
    ) : this(File(directoryPath), recursive, fileExtensions, encoding, metadata)

    override suspend fun load(): List<Document> {
        logger.debug { "Loading documents from directory: ${directory.absolutePath}" }

        if (!directory.exists()) {
            logger.error { "Directory does not exist: ${directory.absolutePath}" }
            return emptyList()
        }

        if (!directory.isDirectory) {
            logger.error { "Path is not a directory: ${directory.absolutePath}" }
            return emptyList()
        }

        val documents = mutableListOf<Document>()

        try {
            val files = if (recursive) {
                directory.walkTopDown().filter { it.isFile }.toList()
            } else {
                directory.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
            }

            val filteredFiles = if (fileExtensions != null) {
                files.filter { file ->
                    fileExtensions.any { ext ->
                        file.name.endsWith(".$ext", ignoreCase = true)
                    }
                }
            } else {
                files
            }

            for (file in filteredFiles) {
                val loader = when {
                    file.name.endsWith(".html", ignoreCase = true) ||
                    file.name.endsWith(".htm", ignoreCase = true) -> {
                        HtmlFileDocumentLoader(file, encoding, metadata)
                    }
                    else -> {
                        TextFileDocumentLoader(file, encoding, metadata)
                    }
                }

                val loadedDocs = loader.load()
                documents.addAll(loadedDocs)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents from directory: ${directory.absolutePath}" }
        }

        return documents
    }
}
