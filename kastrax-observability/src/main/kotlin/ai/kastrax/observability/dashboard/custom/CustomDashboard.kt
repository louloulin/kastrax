package ai.kastrax.observability.dashboard.custom

import ai.kastrax.observability.dashboard.Dashboard
import ai.kastrax.observability.dashboard.DashboardType
import java.util.UUID

/**
 * 自定义仪表板实现。
 *
 * @property name 仪表板名称
 * @property description 仪表板描述
 * @property url 仪表板URL
 * @property id 仪表板ID
 * @property config 仪表板配置
 */
class CustomDashboard(
    private val name: String,
    private val description: String,
    private val url: String,
    private val id: String = UUID.randomUUID().toString(),
    private val config: CustomDashboardConfig
) : Dashboard {
    override fun getName(): String = name

    override fun getDescription(): String = description

    override fun getUrl(): String = url

    override fun getType(): DashboardType = DashboardType.CUSTOM

    /**
     * 获取仪表板ID。
     *
     * @return 仪表板ID
     */
    fun getId(): String = id

    /**
     * 获取仪表板配置。
     *
     * @return 仪表板配置
     */
    fun getConfig(): CustomDashboardConfig = config

    override fun exportConfig(): String {
        return config.toJson()
    }
}

/**
 * 自定义仪表板配置。
 *
 * @property title 仪表板标题
 * @property id 仪表板ID
 * @property theme 主题
 * @property refreshInterval 刷新间隔（秒）
 * @property widgets 小部件列表
 */
data class CustomDashboardConfig(
    val title: String,
    val id: String,
    val theme: String = "light",
    val refreshInterval: Int = 30,
    val widgets: List<DashboardWidget> = emptyList()
) {
    /**
     * 转换为JSON字符串。
     *
     * @return JSON字符串
     */
    fun toJson(): String {
        val widgetsJson = widgets.joinToString(",") { it.toJson() }

        return """
            {
                "id": "$id",
                "title": "$title",
                "theme": "$theme",
                "refreshInterval": $refreshInterval,
                "widgets": [$widgetsJson]
            }
        """.trimIndent()
    }
}

/**
 * 仪表板小部件。
 *
 * @property id 小部件ID
 * @property title 小部件标题
 * @property type 小部件类型
 * @property x X坐标
 * @property y Y坐标
 * @property width 宽度
 * @property height 高度
 * @property dataSource 数据源
 * @property config 小部件配置
 */
data class DashboardWidget(
    val id: String,
    val title: String,
    val type: WidgetType,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val dataSource: String,
    val config: Map<String, Any> = emptyMap()
) {
    /**
     * 转换为JSON字符串。
     *
     * @return JSON字符串
     */
    fun toJson(): String {
        val configJson = config.entries.joinToString(",") { (key, value) ->
            when (value) {
                is String -> "\"$key\":\"$value\""
                is Number -> "\"$key\":$value"
                is Boolean -> "\"$key\":$value"
                else -> "\"$key\":\"$value\""
            }
        }

        return """
            {
                "id": "$id",
                "title": "$title",
                "type": "${type.name.lowercase()}",
                "x": $x,
                "y": $y,
                "width": $width,
                "height": $height,
                "dataSource": "$dataSource",
                "config": {$configJson}
            }
        """.trimIndent()
    }
}

/**
 * 小部件类型枚举。
 */
enum class WidgetType {
    /**
     * 图表小部件。
     */
    CHART,

    /**
     * 表格小部件。
     */
    TABLE,

    /**
     * 统计小部件。
     */
    STAT,

    /**
     * 文本小部件。
     */
    TEXT,

    /**
     * 警报小部件。
     */
    ALERT
}
