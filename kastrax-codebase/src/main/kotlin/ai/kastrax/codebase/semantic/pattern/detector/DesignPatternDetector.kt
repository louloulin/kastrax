package ai.kastrax.codebase.semantic.pattern.detector

import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.pattern.PatternCategory
import ai.kastrax.codebase.semantic.pattern.PatternMatch
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * 设计模式检测器
 *
 * 检测代码中的设计模式
 */
class DesignPatternDetector : AbstractFlowGraphPatternDetector(
    PatternCategory.DESIGN_PATTERN,
    listOf(
        "singleton" to "单例模式",
        "factory" to "工厂模式",
        "abstract_factory" to "抽象工厂模式",
        "builder" to "建造者模式",
        "prototype" to "原型模式",
        "adapter" to "适配器模式",
        "bridge" to "桥接模式",
        "composite" to "组合模式",
        "decorator" to "装饰器模式",
        "facade" to "外观模式",
        "flyweight" to "享元模式",
        "proxy" to "代理模式",
        "chain_of_responsibility" to "责任链模式",
        "command" to "命令模式",
        "interpreter" to "解释器模式",
        "iterator" to "迭代器模式",
        "mediator" to "中介者模式",
        "memento" to "备忘录模式",
        "observer" to "观察者模式",
        "state" to "状态模式",
        "strategy" to "策略模式",
        "template_method" to "模板方法模式",
        "visitor" to "访问者模式"
    )
) {
    private val logger = KotlinLogging.logger {}

    override suspend fun detectPatterns(element: CodeElement): List<PatternMatch> {
        logger.debug { "检测设计模式: ${element.qualifiedName}" }

        // 只对类和包进行检测
        if (element.type != CodeElementType.CLASS && element.type != CodeElementType.PACKAGE) {
            return emptyList()
        }

        val results = mutableListOf<PatternMatch>()

        // 检测单例模式
        detectSingletonPattern(element)?.let { results.add(it) }

        // 检测工厂模式
        detectFactoryPattern(element)?.let { results.add(it) }

        // 检测建造者模式
        detectBuilderPattern(element)?.let { results.add(it) }

        // 检测观察者模式
        detectObserverPattern(element)?.let { results.add(it) }

        // 检测策略模式
        detectStrategyPattern(element)?.let { results.add(it) }

        // 检测装饰器模式
        detectDecoratorPattern(element)?.let { results.add(it) }

        return results
    }

    override suspend fun detectPatternsFromFlowGraph(flowGraph: FlowGraph): List<PatternMatch> {
        logger.debug { "从流图检测设计模式: ${flowGraph.name}" }

        val results = mutableListOf<PatternMatch>()

        // 从流图检测命令模式
        detectCommandPatternFromFlowGraph(flowGraph)?.let { results.add(it) }

        // 从流图检测责任链模式
        detectChainOfResponsibilityFromFlowGraph(flowGraph)?.let { results.add(it) }

        // 从流图检测状态模式
        detectStatePatternFromFlowGraph(flowGraph)?.let { results.add(it) }

        return results
    }

    /**
     * 检测单例模式
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectSingletonPattern(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }

        // 检查是否有私有构造函数
        val hasPrivateConstructor = element.children.any { child ->
            child.type == CodeElementType.CONSTRUCTOR &&
            child.modifiers.any { it.name == "private" }
        }

        // 检查是否有静态实例字段
        val hasStaticInstanceField = element.children.any { child ->
            child.type == CodeElementType.FIELD &&
            child.modifiers.any { it.name == "static" } &&
            child.type == element.type
        }

        // 检查是否有获取实例的静态方法
        val hasStaticGetInstanceMethod = element.children.any { child ->
            child.type == CodeElementType.METHOD &&
            child.modifiers.any { it.name == "static" } &&
            (child.name.startsWith("get") || child.name.startsWith("instance") || child.name == "getInstance")
            // 注释掉返回类型检查，因为当前 CodeElement 没有 returnType 属性
            // && child.returnType == element.name
        }

        // 如果满足单例模式的特征，创建匹配结果
        if (hasPrivateConstructor && (hasStaticInstanceField || hasStaticGetInstanceMethod)) {
            return PatternMatch(
                patternId = "singleton",
                patternName = "单例模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.9,
                elements = listOf(element),
                description = "类 ${element.name} 实现了单例模式，确保只有一个实例被创建。",
                suggestion = "确保单例在多线程环境下是线程安全的。考虑使用枚举或静态内部类实现更安全的单例。"
            )
        }

        return null
    }

    /**
     * 检测工厂模式
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectFactoryPattern(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }

        // 检查类名是否包含"Factory"
        val nameContainsFactory = element.name.contains("Factory", ignoreCase = true)

        // 检查是否有创建对象的方法
        val hasCreateMethods = element.children.any { child ->
            child.type == CodeElementType.METHOD &&
            (child.name.startsWith("create") ||
             child.name.startsWith("get") ||
             child.name.startsWith("new") ||
             child.name.startsWith("make"))
        }

        // 如果满足工厂模式的特征，创建匹配结果
        if (nameContainsFactory && hasCreateMethods) {
            return PatternMatch(
                patternId = "factory",
                patternName = "工厂模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.8,
                elements = listOf(element),
                description = "类 ${element.name} 实现了工厂模式，用于创建对象而不暴露创建逻辑。",
                suggestion = "考虑使用依赖注入来提高灵活性和可测试性。"
            )
        }

        return null
    }

    /**
     * 检测建造者模式
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectBuilderPattern(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }

        // 检查是否有名为"Builder"的内部类
        val hasBuilderInnerClass = element.children.any { child ->
            child.type == CodeElementType.CLASS &&
            child.name == "Builder"
        }

        // 检查是否有链式方法（返回this的方法）
        val hasChainMethods = element.children.any { child ->
            child.type == CodeElementType.METHOD &&
            child.name.contains("with", ignoreCase = true) ||
            child.name.contains("build", ignoreCase = true) ||
            child.name.contains("set", ignoreCase = true)
            // 注释掉返回类型检查，因为当前 CodeElement 没有 returnType 属性
            // && child.returnType == element.name
        }

        // 检查类名是否包含"Builder"
        val nameContainsBuilder = element.name.contains("Builder", ignoreCase = true)

        // 如果满足建造者模式的特征，创建匹配结果
        if (hasBuilderInnerClass || (hasChainMethods && nameContainsBuilder)) {
            return PatternMatch(
                patternId = "builder",
                patternName = "建造者模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.85,
                elements = listOf(element),
                description = "类 ${element.name} 实现了建造者模式，用于构建复杂对象。",
                suggestion = "确保建造者模式的实现是不可变的，并考虑使用方法引用简化客户端代码。"
            )
        }

        return null
    }

    /**
     * 检测观察者模式
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectObserverPattern(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }

        // 检查是否实现了Observer接口或类名包含Observer
        // 注释掉接口检查，因为当前 CodeElement 没有 interfaces 属性
        // val isObserver = element.interfaces.any { it.contains("Observer") } ||
        val isObserver = element.name.contains("Observer", ignoreCase = true) ||
                         element.name.contains("Listener", ignoreCase = true)

        // 检查是否有添加/删除观察者的方法
        val hasObserverMethods = element.children.any { child ->
            child.type == CodeElementType.METHOD &&
            (child.name.contains("add") || child.name.contains("remove") ||
             child.name.contains("register") || child.name.contains("unregister")) &&
            (child.name.contains("Observer") || child.name.contains("Listener"))
        }

        // 检查是否有通知方法
        val hasNotifyMethod = element.children.any { child ->
            child.type == CodeElementType.METHOD &&
            (child.name.contains("notify") || child.name.contains("update") ||
             child.name.contains("publish") || child.name.contains("fire") ||
             child.name.contains("trigger"))
        }

        // 如果满足观察者模式的特征，创建匹配结果
        if ((isObserver || hasObserverMethods) && hasNotifyMethod) {
            return PatternMatch(
                patternId = "observer",
                patternName = "观察者模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.8,
                elements = listOf(element),
                description = "类 ${element.name} 实现了观察者模式，用于对象间的一对多依赖关系。",
                suggestion = "考虑使用事件总线或响应式编程框架来简化观察者模式的实现。"
            )
        }

        return null
    }

    /**
     * 检测策略模式
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectStrategyPattern(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }

        // 检查是否是接口或抽象类
        val isInterfaceOrAbstract = element.type == CodeElementType.INTERFACE ||
                                   element.modifiers.any { it.name == "abstract" }

        // 检查类名是否包含Strategy
        val nameContainsStrategy = element.name.contains("Strategy", ignoreCase = true)

        // 检查是否有多个实现类（简化检查，因为当前 CodeElement 没有 superClass 属性）
        val hasMultipleImplementations = element.children.count {
            it.type == CodeElementType.CLASS
        } > 1

        // 如果满足策略模式的特征，创建匹配结果
        if (isInterfaceOrAbstract && (nameContainsStrategy || hasMultipleImplementations)) {
            return PatternMatch(
                patternId = "strategy",
                patternName = "策略模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.75,
                elements = listOf(element),
                description = "类 ${element.name} 实现了策略模式，定义了一系列算法并使它们可互换。",
                suggestion = "考虑使用函数式接口和Lambda表达式简化策略模式的实现。"
            )
        }

        return null
    }

    /**
     * 检测装饰器模式
     *
     * @param element 代码元素
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectDecoratorPattern(element: CodeElement): PatternMatch? {
        if (element.type != CodeElementType.CLASS) {
            return null
        }

        // 检查类名是否包含Decorator或Wrapper
        val nameContainsDecorator = element.name.contains("Decorator", ignoreCase = true) ||
                                   element.name.contains("Wrapper", ignoreCase = true)

        // 检查是否有字段（简化检查，因为当前 CodeElement 没有 superClass 属性）
        val hasSameTypeField = element.children.any { child ->
            child.type == CodeElementType.FIELD
        }

        // 检查构造函数（简化检查，因为当前 CodeElement 没有 parameters 和 superClass 属性）
        val constructorAcceptsSameType = element.children.any { child ->
            child.type == CodeElementType.CONSTRUCTOR
        }

        // 如果满足装饰器模式的特征，创建匹配结果
        if (nameContainsDecorator || (hasSameTypeField && constructorAcceptsSameType)) {
            return PatternMatch(
                patternId = "decorator",
                patternName = "装饰器模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.8,
                elements = listOf(element),
                description = "类 ${element.name} 实现了装饰器模式，动态地向对象添加职责。",
                suggestion = "确保装饰器类与被装饰类实现相同的接口，并考虑使用组合而非继承。"
            )
        }

        return null
    }

    /**
     * 从流图检测命令模式
     *
     * @param flowGraph 流图
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectCommandPatternFromFlowGraph(flowGraph: FlowGraph): PatternMatch? {
        // 检查流图中是否有命令执行的特征
        val hasExecuteMethod = flowGraph.nodes.values.any { node ->
            node.element?.type == CodeElementType.METHOD &&
            (node.element.name == "execute" || node.element.name == "run" ||
             node.element.name == "perform" || node.element.name == "doExecute")
        }

        // 检查是否有命令调用者
        val hasInvoker = flowGraph.nodes.values.any { node ->
            node.element?.type == CodeElementType.CLASS &&
            (node.element.name.contains("Invoker") || node.element.name.contains("Commander"))
        }

        // 如果满足命令模式的特征，创建匹配结果
        if (hasExecuteMethod && hasInvoker) {
            val elements = flowGraph.nodes.values
                .filter { it.element != null }
                .map { it.element!! }
                .distinct()

            return PatternMatch(
                patternId = "command",
                patternName = "命令模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.75,
                elements = elements,
                description = "流图中检测到命令模式，将请求封装为对象，从而使用户可以参数化不同请求。",
                suggestion = "考虑使用函数式接口简化命令模式的实现。"
            )
        }

        return null
    }

    /**
     * 从流图检测责任链模式
     *
     * @param flowGraph 流图
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectChainOfResponsibilityFromFlowGraph(flowGraph: FlowGraph): PatternMatch? {
        // 检查流图中是否有链式调用的特征
        val hasChainStructure = flowGraph.edges.values.any { edge ->
            edge.metadata["isChain"] == true
        }

        // 检查是否有处理方法
        val hasHandleMethod = flowGraph.nodes.values.any { node ->
            node.element?.type == CodeElementType.METHOD &&
            (node.element.name.contains("handle") || node.element.name.contains("process"))
        }

        // 如果满足责任链模式的特征，创建匹配结果
        if (hasChainStructure && hasHandleMethod) {
            val elements = flowGraph.nodes.values
                .filter { it.element != null }
                .map { it.element!! }
                .distinct()

            return PatternMatch(
                patternId = "chain_of_responsibility",
                patternName = "责任链模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.7,
                elements = elements,
                description = "流图中检测到责任链模式，使多个对象都有机会处理请求。",
                suggestion = "确保链中的处理器能够正确地传递请求，并考虑使用Builder模式构建责任链。"
            )
        }

        return null
    }

    /**
     * 从流图检测状态模式
     *
     * @param flowGraph 流图
     * @return 模式匹配结果，如果不匹配则返回null
     */
    private fun detectStatePatternFromFlowGraph(flowGraph: FlowGraph): PatternMatch? {
        // 检查流图中是否有状态转换的特征
        val hasStateTransitions = flowGraph.edges.values.count { edge ->
            edge.metadata["isStateTransition"] == true
        } >= 2

        // 检查是否有状态类
        val hasStateClasses = flowGraph.nodes.values.any { node ->
            node.element?.type == CodeElementType.CLASS &&
            node.element.name.contains("State")
        }

        // 如果满足状态模式的特征，创建匹配结果
        if (hasStateTransitions || hasStateClasses) {
            val elements = flowGraph.nodes.values
                .filter { it.element != null }
                .map { it.element!! }
                .distinct()

            return PatternMatch(
                patternId = "state",
                patternName = "状态模式",
                category = PatternCategory.DESIGN_PATTERN,
                confidence = 0.7,
                elements = elements,
                description = "流图中检测到状态模式，允许对象在内部状态改变时改变其行为。",
                suggestion = "确保状态转换逻辑清晰，并考虑使用状态机框架简化复杂状态管理。"
            )
        }

        return null
    }
}
