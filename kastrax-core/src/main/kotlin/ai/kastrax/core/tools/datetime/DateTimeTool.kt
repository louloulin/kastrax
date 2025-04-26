package ai.kastrax.core.tools.datetime

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import kotlinx.serialization.json.*
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
class DateTimeTool : Tool {

    private val logger = KotlinLogging.logger {}

    override val id: String = "datetime"
    override val name: String = "日期时间工具"
    override val description: String = """
        执行日期时间相关操作。

        支持的操作包括：
        - current: 获取当前日期时间，可选参数 format（格式化模式）和 timezone（时区）
        - format: 格式化日期时间，必需参数 datetime（日期时间字符串）和 format（格式化模式）
        - parse: 解析日期时间，必需参数 datetime（日期时间字符串）和 format（格式化模式）
        - add: 日期时间加法，必需参数 datetime（日期时间字符串）、amount（数量）和 unit（单位：years, months, days, hours, minutes, seconds）
        - subtract: 日期时间减法，必需参数 datetime（日期时间字符串）、amount（数量）和 unit（单位：years, months, days, hours, minutes, seconds）
        - difference: 计算两个日期时间之间的差异，必需参数 datetime1（第一个日期时间字符串）和 datetime2（第二个日期时间字符串）
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

    override val inputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("operation") {
                put("type", "string")
                put("description", "要执行的操作")
                putJsonArray("enum") {
                    add("current")
                    add("format")
                    add("parse")
                    add("add")
                    add("subtract")
                    add("difference")
                    add("convert")
                }
            }
            putJsonObject("datetime") {
                put("type", "string")
                put("description", "日期时间字符串")
            }
            putJsonObject("datetime1") {
                put("type", "string")
                put("description", "第一个日期时间字符串（用于difference操作）")
            }
            putJsonObject("datetime2") {
                put("type", "string")
                put("description", "第二个日期时间字符串（用于difference操作）")
            }
            putJsonObject("format") {
                put("type", "string")
                put("description", "日期时间格式化模式")
            }
            putJsonObject("amount") {
                put("type", "integer")
                put("description", "数量（用于add和subtract操作）")
            }
            putJsonObject("unit") {
                put("type", "string")
                put("description", "时间单位（用于add、subtract和difference操作）")
                putJsonArray("enum") {
                    add("years")
                    add("months")
                    add("days")
                    add("hours")
                    add("minutes")
                    add("seconds")
                }
            }
            putJsonObject("timezone") {
                put("type", "string")
                put("description", "时区")
            }
            putJsonObject("targetTimezone") {
                put("type", "string")
                put("description", "目标时区（用于convert操作）")
            }
        }
        putJsonArray("required") {
            add("operation")
        }
    }

    override val outputSchema: JsonElement = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("result") {
                put("type", "string")
                put("description", "操作结果")
            }
            putJsonObject("description") {
                put("type", "string")
                put("description", "结果描述")
            }
        }
    }

    override suspend fun execute(input: JsonElement): JsonElement {
        logger.info { "执行日期时间操作: $input" }

        try {
            val inputObj = input.jsonObject
            val operation = inputObj["operation"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少必需的 operation 参数")

            return when (operation) {
                "current" -> getCurrentDateTime(inputObj)
                "format" -> formatDateTime(inputObj)
                "parse" -> parseDateTime(inputObj)
                "add" -> addToDateTime(inputObj)
                "subtract" -> subtractFromDateTime(inputObj)
                "difference" -> calculateDateTimeDifference(inputObj)
                "convert" -> convertTimeZone(inputObj)
                else -> throw IllegalArgumentException("不支持的操作: $operation")
            }
        } catch (e: Exception) {
            logger.error(e) { "执行日期时间操作时发生错误" }
            return buildJsonObject {
                put("result", "")
                put("description", "错误: ${e.message}")
            }
        }
    }

    /**
     * 获取当前日期时间。
     */
    private fun getCurrentDateTime(input: JsonObject): JsonElement {
        val format = input["format"]?.jsonPrimitive?.contentOrNull
        val timezone = input["timezone"]?.jsonPrimitive?.contentOrNull

        val now = if (timezone != null) {
            ZonedDateTime.now(ZoneId.of(timezone))
        } else {
            LocalDateTime.now()
        }

        val result = if (format != null) {
            val formatter = DateTimeFormatter.ofPattern(format)
            formatter.format(now)
        } else {
            now.toString()
        }

        return buildJsonObject {
            put("result", result)
            put("description", "当前日期时间${if (timezone != null) "（${timezone}时区）" else ""}")
        }
    }

    /**
     * 格式化日期时间。
     */
    private fun formatDateTime(input: JsonObject): JsonElement {
        val datetime = input["datetime"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("format 操作需要 datetime 参数")
        val format = input["format"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("format 操作需要 format 参数")

        try {
            val dateTime = LocalDateTime.parse(datetime)
            val formatter = DateTimeFormatter.ofPattern(format)
            val result = dateTime.format(formatter)

            return buildJsonObject {
                put("result", result)
                put("description", "格式化后的日期时间")
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
        }
    }

    /**
     * 解析日期时间。
     */
    private fun parseDateTime(input: JsonObject): JsonElement {
        val datetime = input["datetime"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("parse 操作需要 datetime 参数")
        val format = input["format"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("parse 操作需要 format 参数")

        try {
            val formatter = DateTimeFormatter.ofPattern(format)
            val dateTime = LocalDateTime.parse(datetime, formatter)

            return buildJsonObject {
                put("result", dateTime.toString())
                put("description", "解析后的日期时间")
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
        }
    }

    /**
     * 日期时间加法。
     */
    private fun addToDateTime(input: JsonObject): JsonElement {
        val datetime = input["datetime"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("add 操作需要 datetime 参数")
        val amount = input["amount"]?.jsonPrimitive?.intOrNull
            ?: throw IllegalArgumentException("add 操作需要 amount 参数")
        val unit = input["unit"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("add 操作需要 unit 参数")
        val format = input["format"]?.jsonPrimitive?.contentOrNull

        try {
            val dateTime = LocalDateTime.parse(datetime)
            val result = when (unit.lowercase()) {
                "years" -> dateTime.plusYears(amount.toLong())
                "months" -> dateTime.plusMonths(amount.toLong())
                "days" -> dateTime.plusDays(amount.toLong())
                "hours" -> dateTime.plusHours(amount.toLong())
                "minutes" -> dateTime.plusMinutes(amount.toLong())
                "seconds" -> dateTime.plusSeconds(amount.toLong())
                else -> throw IllegalArgumentException("不支持的时间单位: $unit")
            }

            val formattedResult = if (format != null) {
                val formatter = DateTimeFormatter.ofPattern(format)
                formatter.format(result)
            } else {
                result.toString()
            }

            return buildJsonObject {
                put("result", formattedResult)
                put("description", "添加 $amount $unit 后的日期时间")
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
        }
    }

    /**
     * 日期时间减法。
     */
    private fun subtractFromDateTime(input: JsonObject): JsonElement {
        val datetime = input["datetime"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("subtract 操作需要 datetime 参数")
        val amount = input["amount"]?.jsonPrimitive?.intOrNull
            ?: throw IllegalArgumentException("subtract 操作需要 amount 参数")
        val unit = input["unit"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("subtract 操作需要 unit 参数")
        val format = input["format"]?.jsonPrimitive?.contentOrNull

        try {
            val dateTime = LocalDateTime.parse(datetime)
            val result = when (unit.lowercase()) {
                "years" -> dateTime.minusYears(amount.toLong())
                "months" -> dateTime.minusMonths(amount.toLong())
                "days" -> dateTime.minusDays(amount.toLong())
                "hours" -> dateTime.minusHours(amount.toLong())
                "minutes" -> dateTime.minusMinutes(amount.toLong())
                "seconds" -> dateTime.minusSeconds(amount.toLong())
                else -> throw IllegalArgumentException("不支持的时间单位: $unit")
            }

            val formattedResult = if (format != null) {
                val formatter = DateTimeFormatter.ofPattern(format)
                formatter.format(result)
            } else {
                result.toString()
            }

            return buildJsonObject {
                put("result", formattedResult)
                put("description", "减去 $amount $unit 后的日期时间")
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
        }
    }

    /**
     * 计算两个日期时间之间的差异。
     */
    private fun calculateDateTimeDifference(input: JsonObject): JsonElement {
        val datetime1 = input["datetime1"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("difference 操作需要 datetime1 参数")
        val datetime2 = input["datetime2"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("difference 操作需要 datetime2 参数")
        val unit = input["unit"]?.jsonPrimitive?.contentOrNull ?: "days"

        try {
            val dateTime1 = LocalDateTime.parse(datetime1)
            val dateTime2 = LocalDateTime.parse(datetime2)

            val difference = when (unit.lowercase()) {
                "years" -> ChronoUnit.YEARS.between(dateTime1, dateTime2)
                "months" -> ChronoUnit.MONTHS.between(dateTime1, dateTime2)
                "days" -> ChronoUnit.DAYS.between(dateTime1, dateTime2)
                "hours" -> ChronoUnit.HOURS.between(dateTime1, dateTime2)
                "minutes" -> ChronoUnit.MINUTES.between(dateTime1, dateTime2)
                "seconds" -> ChronoUnit.SECONDS.between(dateTime1, dateTime2)
                else -> throw IllegalArgumentException("不支持的时间单位: $unit")
            }

            return buildJsonObject {
                put("result", difference.toString())
                put("description", "两个日期时间之间相差的 $unit 数")
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
        }
    }

    /**
     * 时区转换。
     */
    private fun convertTimeZone(input: JsonObject): JsonElement {
        val datetime = input["datetime"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("convert 操作需要 datetime 参数")
        val timezone = input["timezone"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("convert 操作需要 timezone 参数")
        val targetTimezone = input["targetTimezone"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException("convert 操作需要 targetTimezone 参数")
        val format = input["format"]?.jsonPrimitive?.contentOrNull

        try {
            val sourceZone = ZoneId.of(timezone)
            val targetZone = ZoneId.of(targetTimezone)

            val dateTime = LocalDateTime.parse(datetime)
            val zonedDateTime = dateTime.atZone(sourceZone)
            val convertedDateTime = zonedDateTime.withZoneSameInstant(targetZone)

            val formattedResult = if (format != null) {
                val formatter = DateTimeFormatter.ofPattern(format)
                formatter.format(convertedDateTime)
            } else {
                convertedDateTime.toString()
            }

            return buildJsonObject {
                put("result", formattedResult)
                put("description", "从 $timezone 转换到 $targetTimezone 的日期时间")
            }
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("无法解析日期时间: ${e.message}")
        } catch (e: ZoneRulesException) {
            throw IllegalArgumentException("无效的时区: ${e.message}")
        }
    }

    companion object {
        /**
         * 创建日期时间工具。
         *
         * @return 日期时间工具实例
         */
        fun create(): Tool {
            return DateTimeTool()
        }
    }
}
