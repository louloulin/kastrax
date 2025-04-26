package ai.kastrax.memory.api

import kotlinx.datetime.Instant

/**
 * 记忆查询选项，用于高级查询和过滤。
 *
 * @property limit 返回的最大消息数量
 * @property offset 分页偏移量
 * @property sortBy 排序字段
 * @property sortDirection 排序方向
 * @property filters 过滤条件
 * @property includeMessageIds 要包含的消息ID列表
 * @property excludeMessageIds 要排除的消息ID列表
 * @property timeRange 时间范围
 * @property processors 要应用的处理器列表
 */
data class MemoryQueryOptions(
    val limit: Int = 10,
    val offset: Int = 0,
    val sortBy: SortField = SortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val filters: List<MessageFilter> = emptyList(),
    val includeMessageIds: List<String> = emptyList(),
    val excludeMessageIds: List<String> = emptyList(),
    val timeRange: TimeRange? = null,
    val processors: List<MemoryProcessor>? = null
)

/**
 * 排序字段枚举。
 */
enum class SortField {
    CREATED_AT,
    PRIORITY,
    ACCESS_COUNT,
    LAST_ACCESSED_AT
}

/**
 * 排序方向枚举。
 */
enum class SortDirection {
    ASC,
    DESC
}

/**
 * 消息过滤器接口。
 */
sealed interface MessageFilter

/**
 * 角色过滤器，用于按消息角色过滤。
 *
 * @property roles 要包含的角色列表
 */
data class RoleFilter(val roles: List<MessageRole>) : MessageFilter

/**
 * 内容过滤器，用于按消息内容过滤。
 *
 * @property query 搜索查询
 * @property matchType 匹配类型
 */
data class ContentFilter(
    val query: String,
    val matchType: ContentMatchType = ContentMatchType.CONTAINS
) : MessageFilter

/**
 * 内容匹配类型枚举。
 */
enum class ContentMatchType {
    CONTAINS,
    EXACT,
    STARTS_WITH,
    ENDS_WITH,
    REGEX
}

/**
 * 元数据过滤器，用于按消息元数据过滤。
 *
 * @property key 元数据键
 * @property value 元数据值
 * @property matchType 匹配类型
 */
data class MetadataFilter(
    val key: String,
    val value: Any,
    val matchType: MetadataMatchType = MetadataMatchType.EQUALS
) : MessageFilter

/**
 * 元数据匹配类型枚举。
 */
enum class MetadataMatchType {
    EQUALS,
    CONTAINS,
    EXISTS,
    NOT_EXISTS
}

/**
 * 优先级过滤器，用于按消息优先级过滤。
 *
 * @property minPriority 最小优先级
 * @property maxPriority 最大优先级
 */
data class PriorityRangeFilter(
    val minPriority: MemoryPriority? = null,
    val maxPriority: MemoryPriority? = null
) : MessageFilter

/**
 * 时间范围，用于按时间过滤消息。
 *
 * @property start 开始时间
 * @property end 结束时间
 */
data class TimeRange(
    val start: Instant? = null,
    val end: Instant? = null
)

/**
 * 上下文选择器，用于选择消息上下文。
 *
 * @property messageId 中心消息ID
 * @property beforeCount 之前的消息数量
 * @property afterCount 之后的消息数量
 */
data class ContextSelector(
    val messageId: String,
    val beforeCount: Int = 2,
    val afterCount: Int = 2
)

/**
 * 线程查询选项，用于高级线程查询和过滤。
 *
 * @property limit 返回的最大线程数量
 * @property offset 分页偏移量
 * @property sortBy 排序字段
 * @property sortDirection 排序方向
 * @property titleFilter 标题过滤器
 * @property timeRange 时间范围
 * @property metadataFilters 元数据过滤器列表
 */
data class ThreadQueryOptions(
    val limit: Int = 20,
    val offset: Int = 0,
    val sortBy: ThreadSortField = ThreadSortField.UPDATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val titleFilter: String? = null,
    val timeRange: TimeRange? = null,
    val metadataFilters: List<MetadataFilter> = emptyList()
)

/**
 * 线程排序字段枚举。
 */
enum class ThreadSortField {
    CREATED_AT,
    UPDATED_AT,
    MESSAGE_COUNT,
    TITLE
}

/**
 * 消息批量操作选项，用于批量操作消息。
 *
 * @property messageIds 要操作的消息ID列表
 * @property filters 过滤条件，用于选择要操作的消息
 * @property threadId 线程ID，如果只操作特定线程的消息
 */
data class MessageBatchOptions(
    val messageIds: List<String> = emptyList(),
    val filters: List<MessageFilter> = emptyList(),
    val threadId: String? = null
)

/**
 * 消息更新选项，用于更新消息。
 *
 * @property priority 新的优先级
 * @property metadata 要更新的元数据
 */
data class MessageUpdateOptions(
    val priority: MemoryPriority? = null,
    val metadata: Map<String, Any>? = null
)
