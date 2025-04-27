package ai.kastrax.core.tools.file

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ZodTool
import ai.kastrax.core.tools.file.ZodFileOperationTool

/**
 * 文件操作工具工厂，用于创建文件操作工具实例。
 */
object FileOperationToolFactory {

    /**
     * 创建文件操作工具。
     *
     * @return 文件操作工具实例
     */
    fun createTool(): Tool {
        return FileOperationTool.create()
    }

    /**
     * 创建文件操作 ZodTool 实例。
     *
     * @return 文件操作 ZodTool 实例
     */
    fun createZodTool(): ZodTool<ZodFileOperationTool.FileOperationInput, ZodFileOperationTool.FileOperationOutput> {
        return ZodFileOperationTool.createZodTool()
    }
}
