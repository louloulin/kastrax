package ai.kastrax.observability.dashboard.grafana

import ai.kastrax.observability.dashboard.Dashboard
import ai.kastrax.observability.dashboard.DashboardType
import java.util.UUID

/**
 * Grafana仪表板实现。
 *
 * @property name 仪表板名称
 * @property description 仪表板描述
 * @property url Grafana仪表板URL
 * @property uid 仪表板UID
 * @property config 仪表板配置
 */
class GrafanaDashboard(
    private val name: String,
    private val description: String,
    private val url: String,
    private val uid: String = UUID.randomUUID().toString(),
    private val config: GrafanaDashboardConfig
) : Dashboard {
    override fun getName(): String = name

    override fun getDescription(): String = description

    override fun getUrl(): String = url

    override fun getType(): DashboardType = DashboardType.GRAFANA

    /**
     * 获取仪表板UID。
     *
     * @return 仪表板UID
     */
    fun getUid(): String = uid

    /**
     * 获取仪表板配置。
     *
     * @return 仪表板配置
     */
    fun getConfig(): GrafanaDashboardConfig = config

    override fun exportConfig(): String {
        return config.toJson()
    }
}

/**
 * Grafana仪表板配置。
 *
 * @property title 仪表板标题
 * @property uid 仪表板UID
 * @property tags 仪表板标签
 * @property timezone 时区
 * @property panels 面板列表
 * @property variables 变量列表
 */
data class GrafanaDashboardConfig(
    val title: String,
    val uid: String,
    val tags: List<String> = emptyList(),
    val timezone: String = "browser",
    val panels: List<GrafanaPanel> = emptyList(),
    val variables: List<GrafanaVariable> = emptyList()
) {
    /**
     * 转换为JSON字符串。
     *
     * @return JSON字符串
     */
    fun toJson(): String {
        val panelsJson = panels.joinToString(",") { it.toJson() }
        val variablesJson = variables.joinToString(",") { it.toJson() }
        val tagsJson = tags.joinToString(",") { "\"$it\"" }

        return """
            {
                "dashboard": {
                    "id": null,
                    "uid": "$uid",
                    "title": "$title",
                    "tags": [$tagsJson],
                    "timezone": "$timezone",
                    "schemaVersion": 30,
                    "version": 1,
                    "refresh": "5s",
                    "panels": [$panelsJson],
                    "templating": {
                        "list": [$variablesJson]
                    },
                    "time": {
                        "from": "now-6h",
                        "to": "now"
                    }
                },
                "folderId": 0,
                "overwrite": true
            }
        """.trimIndent()
    }
}

/**
 * Grafana面板。
 *
 * @property id 面板ID
 * @property title 面板标题
 * @property type 面板类型
 * @property datasource 数据源
 * @property targets 查询目标
 */
data class GrafanaPanel(
    val id: Int,
    val title: String,
    val type: String,
    val datasource: String,
    val targets: List<GrafanaTarget> = emptyList()
) {
    /**
     * 转换为JSON字符串。
     *
     * @return JSON字符串
     */
    fun toJson(): String {
        val targetsJson = targets.joinToString(",") { it.toJson() }

        return """
            {
                "id": $id,
                "title": "$title",
                "type": "$type",
                "datasource": "$datasource",
                "targets": [$targetsJson],
                "gridPos": {
                    "h": 8,
                    "w": 12,
                    "x": 0,
                    "y": 0
                }
            }
        """.trimIndent()
    }
}

/**
 * Grafana查询目标。
 *
 * @property refId 引用ID
 * @property expr 查询表达式
 * @property legendFormat 图例格式
 */
data class GrafanaTarget(
    val refId: String,
    val expr: String,
    val legendFormat: String = ""
) {
    /**
     * 转换为JSON字符串。
     *
     * @return JSON字符串
     */
    fun toJson(): String {
        return """
            {
                "refId": "$refId",
                "expr": "$expr",
                "legendFormat": "$legendFormat"
            }
        """.trimIndent()
    }
}

/**
 * Grafana变量。
 *
 * @property name 变量名称
 * @property label 变量标签
 * @property query 查询
 * @property type 变量类型
 */
data class GrafanaVariable(
    val name: String,
    val label: String,
    val query: String,
    val type: String = "query"
) {
    /**
     * 转换为JSON字符串。
     *
     * @return JSON字符串
     */
    fun toJson(): String {
        return """
            {
                "name": "$name",
                "label": "$label",
                "query": "$query",
                "type": "$type",
                "refresh": 1,
                "sort": 0,
                "multi": false
            }
        """.trimIndent()
    }
}
