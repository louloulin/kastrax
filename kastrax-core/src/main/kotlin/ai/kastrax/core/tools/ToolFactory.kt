package ai.kastrax.core.tools

import ai.kastrax.core.tools.file.FileSystemTool
import ai.kastrax.core.tools.web.WebSearchTool

/**
 * 工具工厂类，用于创建各种工具的实例。
 */
object ToolFactory {
    /**
     * 创建 Web 搜索工具。
     *
     * @param apiKey API 密钥（可选）
     * @param searchEngine 搜索引擎
     * @param maxResults 最大结果数
     * @return Web 搜索工具实例
     */
    fun createWebSearchTool(
        apiKey: String? = null,
        searchEngine: WebSearchTool.SearchEngine = WebSearchTool.SearchEngine.MOCK,
        maxResults: Int = 5
    ): Tool {
        return WebSearchTool.create(
            apiKey = apiKey,
            searchEngine = searchEngine,
            maxResults = maxResults
        )
    }
    
    /**
     * 创建文件系统工具。
     *
     * @param rootPath 根路径
     * @param allowAbsolutePaths 是否允许绝对路径
     * @return 文件系统工具实例
     */
    fun createFileSystemTool(
        rootPath: String = ".",
        allowAbsolutePaths: Boolean = false
    ): Tool {
        return FileSystemTool.create(
            rootPath = rootPath,
            allowAbsolutePaths = allowAbsolutePaths
        )
    }
}
