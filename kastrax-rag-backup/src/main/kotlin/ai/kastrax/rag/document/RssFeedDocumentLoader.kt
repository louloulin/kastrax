package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import org.dom4j.Document as Dom4jDocument
import org.dom4j.Element
import org.dom4j.io.SAXReader
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

private val logger = KotlinLogging.logger {}

/**
 * RSS/Atom Feed文档加载器，从RSS或Atom Feed加载文档。
 *
 * @property url Feed的URL
 * @property metadata 要添加到文档的元数据
 * @property documentPerItem 是否为每个Feed项创建一个文档，默认为true
 * @property includeContent 是否包含内容，默认为true
 * @property includeDescription 是否包含描述，默认为true
 * @property maxItems 最大项数，默认为50
 * @property timeout 请求超时时间（毫秒），默认为30000
 */
class RssFeedDocumentLoader(
    private val url: URL,
    private val metadata: Map<String, Any> = emptyMap(),
    private val documentPerItem: Boolean = true,
    private val includeContent: Boolean = true,
    private val includeDescription: Boolean = true,
    private val maxItems: Int = 50,
    private val timeout: Int = 30000
) : DocumentLoader {

    constructor(
        urlString: String,
        metadata: Map<String, Any> = emptyMap(),
        documentPerItem: Boolean = true,
        includeContent: Boolean = true,
        includeDescription: Boolean = true,
        maxItems: Int = 50,
        timeout: Int = 30000
    ) : this(
        URL(urlString), metadata, documentPerItem,
        includeContent, includeDescription, maxItems, timeout
    )

    override suspend fun load(): List<Document> {
        logger.debug { "Loading documents from RSS/Atom feed: $url" }

        return try {
            // 打开连接
            val connection = url.openConnection()
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            
            connection.getInputStream().use { inputStream ->
                loadFromInputStream(inputStream)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents from RSS/Atom feed: $url" }
            emptyList()
        }
    }

    /**
     * 从输入流加载文档。
     */
    private fun loadFromInputStream(inputStream: InputStream): List<Document> {
        val reader = SAXReader()
        val xmlDocument = reader.read(inputStream)
        val rootElement = xmlDocument.rootElement
        
        // 检测Feed类型（RSS或Atom）
        val isFeedAtom = rootElement.name.equals("feed", ignoreCase = true)
        val isFeedRss = rootElement.name.equals("rss", ignoreCase = true)
        
        if (!isFeedAtom && !isFeedRss) {
            logger.error { "Unknown feed type: ${rootElement.name}" }
            return emptyList()
        }
        
        // 获取Feed信息
        val feedInfo = extractFeedInfo(xmlDocument, isFeedAtom)
        
        // 基础元数据
        val baseMetadata = mutableMapOf(
            "source" to url.toString(),
            "feed_type" to if (isFeedAtom) "atom" else "rss",
            "feed_title" to (feedInfo["title"] ?: ""),
            "feed_link" to (feedInfo["link"] ?: ""),
            "feed_description" to (feedInfo["description"] ?: "")
        )
        
        // 合并用户提供的元数据
        val fullMetadata = baseMetadata + metadata
        
        // 获取Feed项
        val items = if (isFeedAtom) {
            extractAtomEntries(xmlDocument)
        } else {
            extractRssItems(xmlDocument)
        }
        
        // 限制项数
        val limitedItems = items.take(maxItems)
        
        return if (documentPerItem) {
            // 为每个项创建一个文档
            limitedItems.mapIndexed { index, item ->
                val itemContent = buildItemContent(item)
                
                val itemMetadata = fullMetadata + mapOf(
                    "item_index" to index,
                    "item_number" to (index + 1),
                    "item_title" to (item["title"] ?: ""),
                    "item_link" to (item["link"] ?: ""),
                    "item_date" to (item["date"] ?: "")
                )
                
                Document(itemContent, itemMetadata)
            }
        } else {
            // 整个Feed作为一个文档
            val content = buildFeedContent(feedInfo, limitedItems)
            
            listOf(Document(content, fullMetadata))
        }
    }

    /**
     * 提取Feed信息。
     */
    private fun extractFeedInfo(document: Dom4jDocument, isAtom: Boolean): Map<String, String> {
        val info = mutableMapOf<String, String>()
        
        if (isAtom) {
            // Atom Feed
            val rootElement = document.rootElement
            
            rootElement.element("title")?.textTrim?.let { info["title"] = it }
            
            // 获取链接
            rootElement.elements("link").find { it.attributeValue("rel") == "alternate" }?.let {
                info["link"] = it.attributeValue("href") ?: ""
            }
            
            rootElement.element("subtitle")?.textTrim?.let { info["description"] = it }
        } else {
            // RSS Feed
            val channel = document.rootElement.element("channel")
            
            channel?.element("title")?.textTrim?.let { info["title"] = it }
            channel?.element("link")?.textTrim?.let { info["link"] = it }
            channel?.element("description")?.textTrim?.let { info["description"] = it }
        }
        
        return info
    }

    /**
     * 提取RSS项。
     */
    private fun extractRssItems(document: Dom4jDocument): List<Map<String, String>> {
        val items = mutableListOf<Map<String, String>>()
        val channel = document.rootElement.element("channel")
        
        channel?.elements("item")?.forEach { itemElement ->
            val item = mutableMapOf<String, String>()
            
            itemElement.element("title")?.textTrim?.let { item["title"] = it }
            itemElement.element("link")?.textTrim?.let { item["link"] = it }
            itemElement.element("description")?.textTrim?.let { item["description"] = it }
            
            // 获取内容
            itemElement.element("content:encoded")?.textTrim?.let { item["content"] = it }
            
            // 获取日期
            val pubDate = itemElement.element("pubDate")?.textTrim
            if (pubDate != null) {
                item["date"] = pubDate
                try {
                    val date = parseRssDate(pubDate)
                    item["timestamp"] = date.time.toString()
                } catch (e: Exception) {
                    logger.warn { "Failed to parse RSS date: $pubDate" }
                }
            }
            
            items.add(item)
        }
        
        return items
    }

    /**
     * 提取Atom条目。
     */
    private fun extractAtomEntries(document: Dom4jDocument): List<Map<String, String>> {
        val entries = mutableListOf<Map<String, String>>()
        
        document.rootElement.elements("entry").forEach { entryElement ->
            val entry = mutableMapOf<String, String>()
            
            entryElement.element("title")?.textTrim?.let { entry["title"] = it }
            
            // 获取链接
            entryElement.elements("link").find { it.attributeValue("rel") == "alternate" }?.let {
                entry["link"] = it.attributeValue("href") ?: ""
            }
            
            entryElement.element("summary")?.textTrim?.let { entry["description"] = it }
            entryElement.element("content")?.textTrim?.let { entry["content"] = it }
            
            // 获取日期
            val updated = entryElement.element("updated")?.textTrim
            if (updated != null) {
                entry["date"] = updated
                try {
                    val date = parseAtomDate(updated)
                    entry["timestamp"] = date.time.toString()
                } catch (e: Exception) {
                    logger.warn { "Failed to parse Atom date: $updated" }
                }
            }
            
            entries.add(entry)
        }
        
        return entries
    }

    /**
     * 构建项内容。
     */
    private fun buildItemContent(item: Map<String, String>): String {
        val builder = StringBuilder()
        
        // 添加标题
        item["title"]?.let {
            builder.append("# ").append(it).append("\n\n")
        }
        
        // 添加日期
        item["date"]?.let {
            builder.append("发布日期: ").append(it).append("\n\n")
        }
        
        // 添加链接
        item["link"]?.let {
            builder.append("链接: ").append(it).append("\n\n")
        }
        
        // 添加内容或描述
        if (includeContent && item.containsKey("content")) {
            builder.append(item["content"])
        } else if (includeDescription && item.containsKey("description")) {
            builder.append(item["description"])
        }
        
        return builder.toString()
    }

    /**
     * 构建Feed内容。
     */
    private fun buildFeedContent(feedInfo: Map<String, String>, items: List<Map<String, String>>): String {
        val builder = StringBuilder()
        
        // 添加Feed标题
        feedInfo["title"]?.let {
            builder.append("# ").append(it).append("\n\n")
        }
        
        // 添加Feed描述
        feedInfo["description"]?.let {
            builder.append(it).append("\n\n")
        }
        
        // 添加Feed链接
        feedInfo["link"]?.let {
            builder.append("Feed链接: ").append(it).append("\n\n")
        }
        
        // 添加项
        items.forEachIndexed { index, item ->
            builder.append("## ").append(index + 1).append(". ")
            
            item["title"]?.let {
                builder.append(it)
            }
            
            builder.append("\n\n")
            
            // 添加日期
            item["date"]?.let {
                builder.append("发布日期: ").append(it).append("\n\n")
            }
            
            // 添加链接
            item["link"]?.let {
                builder.append("链接: ").append(it).append("\n\n")
            }
            
            // 添加内容或描述
            if (includeContent && item.containsKey("content")) {
                builder.append(item["content"])
            } else if (includeDescription && item.containsKey("description")) {
                builder.append(item["description"])
            }
            
            builder.append("\n\n")
        }
        
        return builder.toString()
    }

    /**
     * 解析RSS日期。
     */
    private fun parseRssDate(dateString: String): Date {
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "EEE, dd MMM yyyy HH:mm:ss",
            "dd MMM yyyy HH:mm:ss Z"
        )
        
        for (format in formats) {
            try {
                val dateFormat = SimpleDateFormat(format, java.util.Locale.ENGLISH)
                return dateFormat.parse(dateString)
            } catch (e: Exception) {
                // 尝试下一个格式
            }
        }
        
        throw IllegalArgumentException("Unable to parse RSS date: $dateString")
    }

    /**
     * 解析Atom日期。
     */
    private fun parseAtomDate(dateString: String): Date {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        )
        
        for (format in formats) {
            try {
                val dateFormat = SimpleDateFormat(format, java.util.Locale.ENGLISH)
                return dateFormat.parse(dateString)
            } catch (e: Exception) {
                // 尝试下一个格式
            }
        }
        
        throw IllegalArgumentException("Unable to parse Atom date: $dateString")
    }
}
