package ai.kastrax.code.model

/**
 * 位置
 *
 * 表示代码位置
 *
 * @property line 行号（1-based）
 * @property column 列号（1-based）
 * @property endLine 结束行号（1-based）
 * @property endColumn 结束列号（1-based）
 */
data class Location(
    val line: Int,
    val column: Int,
    val endLine: Int = line,
    val endColumn: Int = column
) {
    /**
     * 获取位置范围字符串
     *
     * @return 位置范围字符串，格式为 "line:column-endLine:endColumn"
     */
    fun toRangeString(): String {
        return if (line == endLine && column == endColumn) {
            "$line:$column"
        } else {
            "$line:$column-$endLine:$endColumn"
        }
    }
    
    override fun toString(): String {
        return toRangeString()
    }
}
