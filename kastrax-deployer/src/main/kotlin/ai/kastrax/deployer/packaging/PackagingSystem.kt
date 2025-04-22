package ai.kastrax.deployer.packaging

import java.io.File
import java.nio.file.Path

/**
 * 打包配置。
 *
 * @property name 包名称
 * @property version 包版本
 * @property mainClass 主类名称
 * @property includeResources 是否包含资源文件
 * @property excludePatterns 排除的文件模式
 */
data class PackagingConfig(
    val name: String,
    val version: String = "1.0.0",
    val mainClass: String? = null,
    val includeResources: Boolean = true,
    val excludePatterns: List<String> = emptyList()
)

/**
 * 打包结果。
 *
 * @property success 是否成功
 * @property packageFile 打包文件
 * @property message 结果消息
 * @property logs 打包日志
 */
data class PackagingResult(
    val success: Boolean,
    val packageFile: File? = null,
    val message: String = "",
    val logs: List<String> = emptyList()
)

/**
 * 打包系统接口。
 */
interface PackagingSystem {
    /**
     * 打包系统名称。
     */
    val name: String
    
    /**
     * 打包应用。
     *
     * @param projectPath 项目路径
     * @param config 打包配置
     * @return 打包结果
     */
    fun packageApplication(projectPath: Path, config: PackagingConfig): PackagingResult
    
    /**
     * 验证打包结果。
     *
     * @param packageFile 打包文件
     * @return 是否有效
     */
    fun validatePackage(packageFile: File): Boolean
}
