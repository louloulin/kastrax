package ai.kastrax.codebase.semantic.pattern.detector

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.pattern.PatternCategory
import ai.kastrax.codebase.semantic.pattern.PatternMatch
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 反模式检测器
 *
 * 检测代码中的反模式
 */
class AntiPatternDetector : AbstractPatternDetector(
    PatternCategory.ANTI_PATTERN,
    listOf(
        "god_class" to "上帝类",
        "spaghetti_code" to "意大利面条代码",
        "shotgun_surgery" to "散弹式修改",
        "feature_envy" to "特性依恋",
        "data_class" to "数据类",
        "blob" to "大泥球",
        "lava_flow" to "岩浆流",
        "golden_hammer" to "金锤子",
        "dead_code" to "死代码",
        "copy_paste" to "复制粘贴编程"
    )
) {
    private val logger = KotlinLogging.logger {}

    override suspend fun detectPatterns(element: CodeElement): List<PatternMatch> {
        logger.debug { "检测反模式: ${element.qualifiedName}" }
        
        val results = mutableListOf<PatternMatch>()
        
        // 检测上帝类
        detectGodClass(element)?.let { results.add(it) }
        
        // 检测意大利面条代码
        detectSpaghettiCode(element)?.let { results.add(it) }
        
        // 检测特性依恋
        detectFeatureEnvy(element)?.let { results.add(it) }
        
        // 检测数据类
        detectDataClass(element)?.let { results.add(it) }
        
        // 检测死代码
        detectDeadCode(element)?.let { results.add(it) }
        
        // 检测复制粘贴编程
        detectCopyPaste(element)?.let { results.add(it) }
        
        return results
    }

    /**
     * 检测上帝类
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectGodClass(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }
        
        // 检查类的方法数量
        val methodCount = element.children.count { it.type == CodeElementType.METHOD }
        
        // 检查类的字段数量
        val fieldCount = element.children.count { it.type == CodeElementType.FIELD }
        
        // 检查类的代码行数
        val linesOfCode = element.location.endLine - element.location.startLine + 1
        
        // 如果满足上帝类的特征，创建匹配结果
        if (methodCount > 20 || fieldCount > 15 || linesOfCode > 500) {
            val confidence = calculateGodClassConfidence(methodCount, fieldCount, linesOfCode)
            
            return PatternMatch(
                patternId = "god_class",
                patternName = "上帝类",
                category = PatternCategory.ANTI_PATTERN,
                confidence = confidence,
                elements = listOf(element),
                description = "类 ${element.name} 可能是一个上帝类，它知道或做的太多。" +
                             "方法数: $methodCount, 字段数: $fieldCount, 代码行数: $linesOfCode",
                suggestion = "考虑将类拆分为多个更小、更专注的类，应用单一职责原则。"
            )
        }
        
        return null
    }

    /**
     * 计算上帝类的置信度
     */
    private fun calculateGodClassConfidence(methodCount: Int, fieldCount: Int, linesOfCode: Int): Double {
        var confidence = 0.0
        
        if (methodCount > 30) confidence += 0.3
        else if (methodCount > 20) confidence += 0.2
        else if (methodCount > 10) confidence += 0.1
        
        if (fieldCount > 20) confidence += 0.3
        else if (fieldCount > 15) confidence += 0.2
        else if (fieldCount > 10) confidence += 0.1
        
        if (linesOfCode > 1000) confidence += 0.4
        else if (linesOfCode > 500) confidence += 0.3
        else if (linesOfCode > 300) confidence += 0.2
        
        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * 检测意大利面条代码
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectSpaghettiCode(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.METHOD && element.type != CodeElementType.FUNCTION) {
            return null
        }
        
        // 检查方法的代码行数
        val linesOfCode = element.location.endLine - element.location.startLine + 1
        
        // 检查方法中的条件语句数量
        val conditionCount = element.children.count { 
            it.type == CodeElementType.IF_STATEMENT || 
            it.type == CodeElementType.SWITCH_STATEMENT 
        }
        
        // 检查方法中的循环语句数量
        val loopCount = element.children.count { 
            it.type == CodeElementType.FOR_STATEMENT || 
            it.type == CodeElementType.WHILE_STATEMENT || 
            it.type == CodeElementType.DO_WHILE_STATEMENT 
        }
        
        // 检查方法的嵌套深度
        val nestingDepth = calculateNestingDepth(element)
        
        // 如果满足意大利面条代码的特征，创建匹配结果
        if (linesOfCode > 100 || conditionCount > 10 || loopCount > 5 || nestingDepth > 4) {
            val confidence = calculateSpaghettiCodeConfidence(linesOfCode, conditionCount, loopCount, nestingDepth)
            
            return PatternMatch(
                patternId = "spaghetti_code",
                patternName = "意大利面条代码",
                category = PatternCategory.ANTI_PATTERN,
                confidence = confidence,
                elements = listOf(element),
                description = "方法 ${element.name} 可能是意大利面条代码，结构混乱且难以理解。" +
                             "代码行数: $linesOfCode, 条件语句数: $conditionCount, " +
                             "循环语句数: $loopCount, 嵌套深度: $nestingDepth",
                suggestion = "重构方法，提取子方法，减少嵌套深度，使用更清晰的控制结构。"
            )
        }
        
        return null
    }

    /**
     * 计算嵌套深度
     */
    private fun calculateNestingDepth(element: CodeElement): Int {
        var maxDepth = 0
        
        fun traverse(node: CodeElement, currentDepth: Int) {
            var newDepth = currentDepth
            
            // 如果是控制结构，增加深度
            if (node.type == CodeElementType.IF_STATEMENT || 
                node.type == CodeElementType.FOR_STATEMENT || 
                node.type == CodeElementType.WHILE_STATEMENT || 
                node.type == CodeElementType.DO_WHILE_STATEMENT || 
                node.type == CodeElementType.SWITCH_STATEMENT || 
                node.type == CodeElementType.TRY_STATEMENT) {
                newDepth++
                maxDepth = maxOf(maxDepth, newDepth)
            }
            
            // 递归遍历子节点
            for (child in node.children) {
                traverse(child, newDepth)
            }
        }
        
        traverse(element, 0)
        return maxDepth
    }

    /**
     * 计算意大利面条代码的置信度
     */
    private fun calculateSpaghettiCodeConfidence(
        linesOfCode: Int, 
        conditionCount: Int, 
        loopCount: Int, 
        nestingDepth: Int
    ): Double {
        var confidence = 0.0
        
        if (linesOfCode > 200) confidence += 0.3
        else if (linesOfCode > 100) confidence += 0.2
        else if (linesOfCode > 50) confidence += 0.1
        
        if (conditionCount > 15) confidence += 0.3
        else if (conditionCount > 10) confidence += 0.2
        else if (conditionCount > 5) confidence += 0.1
        
        if (loopCount > 8) confidence += 0.2
        else if (loopCount > 5) confidence += 0.15
        else if (loopCount > 3) confidence += 0.1
        
        if (nestingDepth > 6) confidence += 0.2
        else if (nestingDepth > 4) confidence += 0.15
        else if (nestingDepth > 3) confidence += 0.1
        
        return confidence.coerceIn(0.0, 1.0)
    }

    /**
     * 检测特性依恋
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectFeatureEnvy(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.METHOD) {
            return null
        }
        
        // 统计方法中对其他类的访问次数
        val externalAccessCount = mutableMapOf<String, Int>()
        
        // 递归遍历方法体，统计对外部类的访问
        fun countExternalAccesses(node: CodeElement) {
            if (node.type == CodeElementType.FIELD_ACCESS || node.type == CodeElementType.METHOD_CALL) {
                val targetClass = node.target?.let { 
                    if (it != element.parent?.name) it else null 
                }
                
                if (targetClass != null) {
                    externalAccessCount[targetClass] = (externalAccessCount[targetClass] ?: 0) + 1
                }
            }
            
            for (child in node.children) {
                countExternalAccesses(child)
            }
        }
        
        countExternalAccesses(element)
        
        // 如果对某个外部类的访问次数超过阈值，可能存在特性依恋
        val mostAccessedClass = externalAccessCount.maxByOrNull { it.value }
        
        if (mostAccessedClass != null && mostAccessedClass.value > 5) {
            val confidence = calculateFeatureEnvyConfidence(mostAccessedClass.value)
            
            return PatternMatch(
                patternId = "feature_envy",
                patternName = "特性依恋",
                category = PatternCategory.ANTI_PATTERN,
                confidence = confidence,
                elements = listOf(element),
                description = "方法 ${element.name} 可能存在特性依恋，过度使用类 ${mostAccessedClass.key} 的功能。" +
                             "访问次数: ${mostAccessedClass.value}",
                suggestion = "考虑将此方法移动到它所依恋的类中，或者提取共享功能到一个新的类。"
            )
        }
        
        return null
    }

    /**
     * 计算特性依恋的置信度
     */
    private fun calculateFeatureEnvyConfidence(accessCount: Int): Double {
        return when {
            accessCount > 15 -> 0.9
            accessCount > 10 -> 0.8
            accessCount > 5 -> 0.7
            else -> 0.6
        }
    }

    /**
     * 检测数据类
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectDataClass(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }
        
        // 检查是否只有字段、getter和setter
        val fieldCount = element.children.count { it.type == CodeElementType.FIELD }
        val methodCount = element.children.count { it.type == CodeElementType.METHOD }
        val getterSetterCount = element.children.count { 
            it.type == CodeElementType.METHOD && 
            (it.name.startsWith("get") || it.name.startsWith("set") || 
             it.name.startsWith("is"))
        }
        
        // 如果几乎所有方法都是getter和setter，可能是数据类
        if (fieldCount > 0 && methodCount > 0 && getterSetterCount.toDouble() / methodCount > 0.8) {
            // 数据类本身不一定是反模式，但在某些情况下可能表明设计不佳
            // 检查是否有其他类过度使用这个数据类
            
            val confidence = 0.7 // 数据类的置信度相对较低，因为它可能是有意设计的
            
            return PatternMatch(
                patternId = "data_class",
                patternName = "数据类",
                category = PatternCategory.ANTI_PATTERN,
                confidence = confidence,
                elements = listOf(element),
                description = "类 ${element.name} 可能是一个纯数据类，只包含数据和访问方法。" +
                             "字段数: $fieldCount, 方法数: $methodCount, getter/setter数: $getterSetterCount",
                suggestion = "考虑将行为移动到数据类中，或者确保这是有意的设计决策。在某些语言中，使用记录类型或数据类可能更合适。"
            )
        }
        
        return null
    }

    /**
     * 检测死代码
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectDeadCode(element: CodeElement): PatternMatch? {
        // 检查是否有被标记为废弃的代码
        val isDeprecated = element.annotations.any { 
            it.name == "Deprecated" || it.name.contains("deprecated", ignoreCase = true) 
        }
        
        // 检查是否有被注释掉的代码块（这需要访问原始代码，这里只是示例）
        val hasCommentedCode = element.comments.any { 
            it.contains("{") && it.contains("}") || 
            it.contains("(") && it.contains(")") ||
            it.contains(";")
        }
        
        // 检查是否有永远不会执行的代码（如return后的代码）
        val hasUnreachableCode = element.children.any { child ->
            child.type == CodeElementType.STATEMENT && 
            child.previous?.type in listOf(
                CodeElementType.RETURN_STATEMENT, 
                CodeElementType.THROW_STATEMENT, 
                CodeElementType.BREAK_STATEMENT, 
                CodeElementType.CONTINUE_STATEMENT
            )
        }
        
        if (isDeprecated || hasCommentedCode || hasUnreachableCode) {
            val confidence = when {
                hasUnreachableCode -> 0.9
                isDeprecated -> 0.8
                hasCommentedCode -> 0.7
                else -> 0.6
            }
            
            return PatternMatch(
                patternId = "dead_code",
                patternName = "死代码",
                category = PatternCategory.ANTI_PATTERN,
                confidence = confidence,
                elements = listOf(element),
                description = "在 ${element.name} 中检测到死代码，这些代码永远不会被执行或已被废弃。",
                suggestion = "删除死代码以提高代码可读性和可维护性。如果代码可能在将来需要，请使用版本控制系统而不是注释。"
            )
        }
        
        return null
    }

    /**
     * 检测复制粘贴编程
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectCopyPaste(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS && element.type != CodeElementType.FILE) {
            return null
        }
        
        // 查找相似的方法
        val methods = element.children.filter { it.type == CodeElementType.METHOD }
        val similarMethodPairs = mutableListOf<Pair<CodeElement, CodeElement>>()
        
        for (i in methods.indices) {
            for (j in i + 1 until methods.size) {
                val method1 = methods[i]
                val method2 = methods[j]
                
                // 计算方法相似度（这里使用简化的方法，实际应使用更复杂的算法）
                val similarity = calculateMethodSimilarity(method1, method2)
                
                if (similarity > 0.8) {
                    similarMethodPairs.add(method1 to method2)
                }
            }
        }
        
        if (similarMethodPairs.isNotEmpty()) {
            val confidence = 0.7 + (0.1 * (similarMethodPairs.size - 1)).coerceAtMost(0.2)
            
            val elements = (similarMethodPairs.flatMap { listOf(it.first, it.second) } + element).distinct()
            
            return PatternMatch(
                patternId = "copy_paste",
                patternName = "复制粘贴编程",
                category = PatternCategory.ANTI_PATTERN,
                confidence = confidence,
                elements = elements,
                description = "在 ${element.name} 中检测到可能的复制粘贴代码，发现 ${similarMethodPairs.size} 对相似方法。",
                suggestion = "提取共同代码到共享方法，应用DRY（不要重复自己）原则。考虑使用模板方法模式或策略模式。"
            )
        }
        
        return null
    }

    /**
     * 计算方法相似度
     */
    private fun calculateMethodSimilarity(method1: CodeElement, method2: CodeElement): Double {
        // 这里使用简化的方法，实际应使用更复杂的算法，如AST比较或代码克隆检测
        
        // 比较方法长度
        val length1 = method1.location.endLine - method1.location.startLine
        val length2 = method2.location.endLine - method2.location.startLine
        val lengthRatio = minOf(length1, length2).toDouble() / maxOf(length1, length2)
        
        // 比较方法结构（子元素类型的序列）
        val structure1 = method1.children.map { it.type }
        val structure2 = method2.children.map { it.type }
        
        val commonStructureSize = structure1.intersect(structure2.toSet()).size
        val structureSimilarity = if (structure1.isEmpty() || structure2.isEmpty()) 0.0
                                 else commonStructureSize.toDouble() / maxOf(structure1.size, structure2.size)
        
        // 综合相似度
        return (lengthRatio * 0.3 + structureSimilarity * 0.7).coerceIn(0.0, 1.0)
    }
}
