package ai.kastrax.core.tools.datetime

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ZodTool

/**
 * 日期时间工具工厂，用于创建日期时间工具实例。
 */
object DateTimeToolFactory {

    /**
     * 创建日期时间工具。
     *
     * @return 日期时间工具实例
     */
    fun createTool(): Tool {
        return ZodDateTimeTool.create()
    }

    /**
     * 创建日期时间 ZodTool 实例。
     *
     * @return 日期时间 ZodTool 实例
     */
    fun createZodTool(): ZodTool<ZodDateTimeTool.DateTimeInput, ZodDateTimeTool.DateTimeOutput> {
        return ZodDateTimeTool.createZodTool()
    }
}
