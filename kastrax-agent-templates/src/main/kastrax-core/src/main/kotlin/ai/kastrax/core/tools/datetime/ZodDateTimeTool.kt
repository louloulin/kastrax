package ai.kastrax.core.tools.datetime

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ZodTool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.zone.ZoneRulesException

/**
 * 日期时间工具，用于执行日期时间相关操作。
 *
 * 支持的操作包括：
 * - 获取当前日期时间
 * - 格式化日期时间
 * - 解析日期时间
 * - 日期时间计算（加减日期）
 * - 日期时间比较
 * - 时区转换
 */
class ZodDateTimeTool : KastraXBase(component = "TOOL", name = "日期时间工具") {

    /**
     * 日期时间工具的输入参数。
     */
    @Serializable
    data class DateTimeInput(
        val operation: String,
        val datetime: String? = null,
        val format: String? = null,
        val amount: Int? = null,
        val unit: String? = null,
        val timezone: String? = null,
        val targetTimezone: String? = null,
        val targetDatetime: String? = null
    )

    /**
     * 日期时间工具的输出结果。
     */
    @Serializable
    data class DateTimeOutput(
        val result: String,
        val description: String
    )

    /**
     * 创建日期时间工具的 ZodTool 实例。
     */
    fun createZodTool(): ZodTool<DateTimeInput, DateTimeOutput> {
        return zodTool {
            id = "datetime"
            name = "日期时间工具"
            description = """
                执行日期时间相关操作。

                支持的操作包括：
                - current: 获取当前日期时间，可选参数 format（格式化模式）和 timezone（时区）
                - format: 格式化日期时间，必需参数 datetime（日期时间字符串）和 format（格式化模式）
                - parse: 解析日期时间，必需参数 datetime（日期时间字符串）和 format（格式化模式）
                - add: 日期时间加法，必需参数 datetime（日期时间字符串）、amount（数量）和 unit（单位：years, months, days, hours, minutes, seconds）
                - subtract: 日期时间减法，必需参数 datetime（日期时间字符串）、amount（数量）和 unit（单位：years, months, days, hours, minutes, seconds）
                - difference: 计算两个日期时间之间的差异，必需参数 datetime（第一个日期时间字符串）和 targetDatetime（第二个日期时间字符串）
                - convert: 时区转换，必需参数 datetime（日期时间字符串）、timezone（源时区）和 targetTimezone（目标时区）

                格式化模式示例：
                - yyyy-MM-dd: 2023-01-31
                - yyyy-MM-dd HH:mm:ss: 2023-01-31 14:30:45
                - dd/MM/yyyy: 31/01/2023
                - HH:mm:ss: 14:30:45

                时区示例：
                - UTC
                - America/New_York
                - Europe/London
                - Asia/Tokyo
                - Asia/Shanghai
            """.trimIndent()

            // 使用类型安全的方式设置输入和输出模式
            @Suppress("UNCHECKED_CAST")
            this.inputSchema = any() as Schema<DateTimeInput, DateTimeInput>

            @Suppress("UNCHECKED_CAST")
            this.outputSchema = any() as Schema<DateTimeOutput, DateTimeOutput>

            execute = { input ->

                when (input.operation) {
                    "current" -> {
                        val now = if (input.timezone != null) {
                            ZonedDateTime.now(ZoneId.of(input.timezone))
                        } else {
                            LocalDateTime.now()
                        }

                        val result = if (input.format != null) {
                            val formatter = DateTimeFormatter.ofPattern(input.format)
                            formatter.format(now)
                        } else {
                            now.toString()
                        }

                        DateTimeOutput(
                            result = result,
                            description = "当前日期时间${if (input.timezone != null) "（${input.timezone}时区）" else ""}"
                        )
                    }
                    "format" -> {
                        requireNotNull(input.datetime) { "format 操作需要 datetime 参数" }
                        requireNotNull(input.format) { "format 操作需要 format 参数" }

                        try {
                            val dateTime = LocalDateTime.parse(input.datetime)
                            val formatter = DateTimeFormatter.ofPattern(input.format)
                            val result = formatter.format(dateTime)

                            DateTimeOutput(
                                result = result,
                                description = "格式化后的日期时间"
                            )
                        } catch (e: DateTimeParseException) {
                            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
                        }
                    }
                    "parse" -> {
                        requireNotNull(input.datetime) { "parse 操作需要 datetime 参数" }
                        requireNotNull(input.format) { "parse 操作需要 format 参数" }

                        try {
                            val formatter = DateTimeFormatter.ofPattern(input.format)
                            val dateTime = LocalDateTime.parse(input.datetime, formatter)

                            DateTimeOutput(
                                result = dateTime.toString(),
                                description = "解析后的日期时间"
                            )
                        } catch (e: DateTimeParseException) {
                            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
                        }
                    }
                    "add" -> {
                        requireNotNull(input.datetime) { "add 操作需要 datetime 参数" }
                        requireNotNull(input.amount) { "add 操作需要 amount 参数" }
                        requireNotNull(input.unit) { "add 操作需要 unit 参数" }

                        try {
                            val dateTime = LocalDateTime.parse(input.datetime)
                            val result = when (input.unit.lowercase()) {
                                "years" -> dateTime.plusYears(input.amount.toLong())
                                "months" -> dateTime.plusMonths(input.amount.toLong())
                                "days" -> dateTime.plusDays(input.amount.toLong())
                                "hours" -> dateTime.plusHours(input.amount.toLong())
                                "minutes" -> dateTime.plusMinutes(input.amount.toLong())
                                "seconds" -> dateTime.plusSeconds(input.amount.toLong())
                                else -> throw IllegalArgumentException("不支持的时间单位: ${input.unit}")
                            }

                            val formattedResult = if (input.format != null) {
                                val formatter = DateTimeFormatter.ofPattern(input.format)
                                formatter.format(result)
                            } else {
                                result.toString()
                            }

                            DateTimeOutput(
                                result = formattedResult,
                                description = "添加 ${input.amount} ${input.unit} 后的日期时间"
                            )
                        } catch (e: DateTimeParseException) {
                            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
                        }
                    }
                    "subtract" -> {
                        requireNotNull(input.datetime) { "subtract 操作需要 datetime 参数" }
                        requireNotNull(input.amount) { "subtract 操作需要 amount 参数" }
                        requireNotNull(input.unit) { "subtract 操作需要 unit 参数" }

                        try {
                            val dateTime = LocalDateTime.parse(input.datetime)
                            val result = when (input.unit.lowercase()) {
                                "years" -> dateTime.minusYears(input.amount.toLong())
                                "months" -> dateTime.minusMonths(input.amount.toLong())
                                "days" -> dateTime.minusDays(input.amount.toLong())
                                "hours" -> dateTime.minusHours(input.amount.toLong())
                                "minutes" -> dateTime.minusMinutes(input.amount.toLong())
                                "seconds" -> dateTime.minusSeconds(input.amount.toLong())
                                else -> throw IllegalArgumentException("不支持的时间单位: ${input.unit}")
                            }

                            val formattedResult = if (input.format != null) {
                                val formatter = DateTimeFormatter.ofPattern(input.format)
                                formatter.format(result)
                            } else {
                                result.toString()
                            }

                            DateTimeOutput(
                                result = formattedResult,
                                description = "减去 ${input.amount} ${input.unit} 后的日期时间"
                            )
                        } catch (e: DateTimeParseException) {
                            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
                        }
                    }
                    "difference" -> {
                        requireNotNull(input.datetime) { "difference 操作需要 datetime 参数" }
                        val targetDatetime = input.targetDatetime ?: throw IllegalArgumentException("difference 操作需要 targetDatetime 参数")

                        try {
                            val dateTime1 = LocalDateTime.parse(input.datetime)
                            val dateTime2 = LocalDateTime.parse(targetDatetime)

                            val unit = input.unit?.lowercase() ?: "days"
                            val difference = when (unit) {
                                "years" -> ChronoUnit.YEARS.between(dateTime1, dateTime2)
                                "months" -> ChronoUnit.MONTHS.between(dateTime1, dateTime2)
                                "days" -> ChronoUnit.DAYS.between(dateTime1, dateTime2)
                                "hours" -> ChronoUnit.HOURS.between(dateTime1, dateTime2)
                                "minutes" -> ChronoUnit.MINUTES.between(dateTime1, dateTime2)
                                "seconds" -> ChronoUnit.SECONDS.between(dateTime1, dateTime2)
                                else -> throw IllegalArgumentException("不支持的时间单位: $unit")
                            }

                            DateTimeOutput(
                                result = difference.toString(),
                                description = "两个日期时间之间相差的 $unit 数"
                            )
                        } catch (e: DateTimeParseException) {
                            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
                        }
                    }
                    "convert" -> {
                        requireNotNull(input.datetime) { "convert 操作需要 datetime 参数" }
                        requireNotNull(input.timezone) { "convert 操作需要 timezone 参数" }
                        requireNotNull(input.targetTimezone) { "convert 操作需要 targetTimezone 参数" }

                        try {
                            val sourceZone = ZoneId.of(input.timezone)
                            val targetZone = ZoneId.of(input.targetTimezone)

                            val dateTime = LocalDateTime.parse(input.datetime)
                            val zonedDateTime = dateTime.atZone(sourceZone)
                            val convertedDateTime = zonedDateTime.withZoneSameInstant(targetZone)

                            val formattedResult = if (input.format != null) {
                                val formatter = DateTimeFormatter.ofPattern(input.format)
                                formatter.format(convertedDateTime)
                            } else {
                                convertedDateTime.toString()
                            }

                            DateTimeOutput(
                                result = formattedResult,
                                description = "从 ${input.timezone} 转换到 ${input.targetTimezone} 的日期时间"
                            )
                        } catch (e: DateTimeParseException) {
                            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
                        } catch (e: ZoneRulesException) {
                            throw IllegalArgumentException("无效的时区: ${e.message}")
                        }
                    }
                    else -> throw IllegalArgumentException("不支持的操作: ${input.operation}")
                }
            }
        }
    }

    /**
     * 创建日期时间工具的 Tool 实例。
     */
    fun createTool(): Tool {
        return createZodTool().toTool()
    }

    companion object {
        /**
         * 创建日期时间工具。
         *
         * @return 日期时间工具实例
         */
        fun create(): Tool {
            return ZodDateTimeTool().createTool()
        }

        /**
         * 创建日期时间 ZodTool 实例。
         *
         * @return 日期时间 ZodTool 实例
         */
        fun createZodTool(): ZodTool<DateTimeInput, DateTimeOutput> {
            return ZodDateTimeTool().createZodTool()
        }
    }
}
