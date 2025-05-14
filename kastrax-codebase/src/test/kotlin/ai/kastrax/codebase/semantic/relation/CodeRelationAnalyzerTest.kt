package ai.kastrax.codebase.semantic.relation

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class CodeRelationAnalyzerTest {
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    private lateinit var elements: List<CodeElement>

    private val testFilePath = Paths.get("/test/path/TestFile.kt")

    @BeforeEach
    fun setUp() {
        // 创建测试元素
        val parentClass = createClassElement("ParentClass")
        val childClass = createClassElement("ChildClass")
        val interface1 = createInterfaceElement("TestInterface")
        val parentMethod = createMethodElement("testMethod", parentClass)
        val childMethod = createMethodElement("testMethod", childClass)
        val field = createFieldElement("testField", childClass)

        elements = listOf(parentClass, childClass, interface1, parentMethod, childMethod, field)

        // 设置元数据
        childClass.metadata["extends"] = "ParentClass"
        childClass.metadata["implements"] = "TestInterface"
        childMethod.metadata["annotations"] = "Override"

        // 创建关系分析器
        relationAnalyzer = CodeRelationAnalyzer(
            CodeRelationAnalyzerConfig(
                analyzeInheritance = true,
                analyzeImplementation = true,
                analyzeOverride = true,
                analyzeUsage = true,
                analyzeDependency = true,
                analyzeReference = true
            )
        )
    }

    @Test
    fun `test analyze inheritance relations`() = runBlocking {
        // 分析关系
        relationAnalyzer.analyzeRelations(elements.first())

        // 获取继承关系
        val childClass = elements.first { it.name == "ChildClass" }
        val parentClass = elements.first { it.name == "ParentClass" }

        val relations = relationAnalyzer.getElementRelations(childClass.id)

        // 验证继承关系
        val inheritanceRelation = relations.find {
            it.sourceId == childClass.id &&
            it.targetId == parentClass.id &&
            it.type == RelationType.INHERITANCE
        }

        assertTrue(inheritanceRelation != null, "应该存在继承关系")
        assertEquals("ChildClass extends ParentClass", inheritanceRelation?.metadata?.get("description"))
    }

    @Test
    fun `test analyze implementation relations`() = runBlocking {
        // 分析关系
        relationAnalyzer.analyzeRelations(elements.first())

        // 获取实现关系
        val childClass = elements.first { it.name == "ChildClass" }
        val interface1 = elements.first { it.name == "TestInterface" }

        val relations = relationAnalyzer.getElementRelations(childClass.id)

        // 验证实现关系
        val implementationRelation = relations.find {
            it.sourceId == childClass.id &&
            it.targetId == interface1.id &&
            it.type == RelationType.IMPLEMENTATION
        }

        assertTrue(implementationRelation != null, "应该存在实现关系")
        assertEquals("ChildClass implements TestInterface", implementationRelation?.metadata?.get("description"))
    }

    @Test
    fun `test analyze override relations`() = runBlocking {
        // 分析关系
        relationAnalyzer.analyzeRelations(elements.first())

        // 获取重写关系
        val childMethod = elements.first { it.name == "testMethod" && it.parent?.name == "ChildClass" }
        val parentMethod = elements.first { it.name == "testMethod" && it.parent?.name == "ParentClass" }

        val relations = relationAnalyzer.getElementRelations(childMethod.id)

        // 验证重写关系
        val overrideRelation = relations.find {
            it.sourceId == childMethod.id &&
            it.targetId == parentMethod.id &&
            it.type == RelationType.OVERRIDE
        }

        assertTrue(overrideRelation != null, "应该存在重写关系")
        assertEquals("testMethod in ChildClass overrides testMethod in ParentClass", overrideRelation?.metadata?.get("description"))
    }

    // 辅助方法：创建类元素
    private fun createClassElement(name: String): CodeElement {
        return CodeElement(
            id = "class:$name",
            name = name,
            qualifiedName = "com.example.$name",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = testFilePath,
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            language = "kotlin"
        )
    }

    // 辅助方法：创建接口元素
    private fun createInterfaceElement(name: String): CodeElement {
        return CodeElement(
            id = "interface:$name",
            name = name,
            qualifiedName = "com.example.$name",
            type = CodeElementType.INTERFACE,
            location = Location(
                filePath = testFilePath,
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            language = "kotlin"
        )
    }

    // 辅助方法：创建方法元素
    private fun createMethodElement(name: String, parent: CodeElement): CodeElement {
        return CodeElement(
            id = "${parent.id}:method:$name",
            name = name,
            qualifiedName = "${parent.qualifiedName}.$name",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = testFilePath,
                startLine = 2,
                startColumn = 1,
                endLine = 5,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            parent = parent,
            language = "kotlin"
        )
    }

    // 辅助方法：创建字段元素
    private fun createFieldElement(name: String, parent: CodeElement): CodeElement {
        return CodeElement(
            id = "${parent.id}:field:$name",
            name = name,
            qualifiedName = "${parent.qualifiedName}.$name",
            type = CodeElementType.FIELD,
            location = Location(
                filePath = testFilePath,
                startLine = 6,
                startColumn = 1,
                endLine = 6,
                endColumn = 20
            ),
            visibility = Visibility.PRIVATE,
            parent = parent,
            language = "kotlin"
        )
    }
}
