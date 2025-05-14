package ai.kastrax.codebase.semantic.pattern

import ai.kastrax.codebase.semantic.model.CodeElement

/**
 * 模式接口
 */
interface Pattern {
    /**
     * 代码元素
     */
    val element: CodeElement

    /**
     * 检测模式
     *
     * @return 是否匹配模式
     */
    fun detect(): Boolean
}
