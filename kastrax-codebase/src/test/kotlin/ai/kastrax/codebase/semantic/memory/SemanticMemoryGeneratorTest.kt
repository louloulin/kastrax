package ai.kastrax.codebase.semantic.memory

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.symbol.model.SymbolGraph
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticMemoryGeneratorTest {

    private lateinit var memoryGenerator: SemanticMemoryGenerator
    private lateinit var symbolGraph: SymbolGraph

    @BeforeEach
    fun setUp() {
        memoryGenerator = SemanticMemoryGenerator(
            config = SemanticMemoryGeneratorConfig(
                generateCodeStructureMemories = true,
                generateSymbolDefinitionMemories = true,
                generateSymbolReferenceMemories = true,
                generateInheritanceMemories = true,
                generateImplementationMemories = true,
                generateCallHierarchyMemories = true,
                generateImportDependencyMemories = true,
                generateLibraryUsageMemories = true,
                generateSemanticRelationMemories = true
            )
        )

        symbolGraph = SymbolGraph("test-graph")
    }

    @Test
    fun `test generating memories from code element`() = runBlocking {
        // 创建代码元素
        val classElement = CodeElement(
            id = "class1",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = Paths.get("TestClass.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 20,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            documentation = "This is a test class."
        )

        val methodElement = CodeElement(
            id = "method1",
            name = "testMethod",
            qualifiedName = "com.example.TestClass.testMethod",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = Paths.get("TestClass.java").toString(),
                startLine = 5,
                startColumn = 5,
                endLine = 8,
                endColumn = 5
            ),
            visibility = Visibility.PUBLIC,
            documentation = "This is a test method.",
            parent = classElement
        )

        val fieldElement = CodeElement(
            id = "field1",
            name = "testField",
            qualifiedName = "com.example.TestClass.testField",
            type = CodeElementType.FIELD,
            location = Location(
                filePath = Paths.get("TestClass.java").toString(),
                startLine = 3,
                startColumn = 5,
                endLine = 3,
                endColumn = 30
            ),
            visibility = Visibility.PRIVATE,
            parent = classElement
        )

        // 添加子元素
        classElement.children.add(methodElement)
        classElement.children.add(fieldElement)

        // 生成记忆
        val memories = memoryGenerator.generateMemoriesFromCodeElement(classElement)

        // 验证记忆
        assertTrue(memories.isNotEmpty())

        // 验证代码结构记忆
        val structureMemories = memories.filter { it.type == MemoryType.CODE_STRUCTURE }
        assertTrue(structureMemories.isNotEmpty())

        val classStructureMemory = structureMemories.find { it.sourceElements.any { element -> element.id == "class1" } }
        assertTrue(classStructureMemory != null)
        assertTrue(classStructureMemory!!.content.contains("TestClass"))
        assertTrue(classStructureMemory.content.contains("2 个子元素"))

        // 验证符号定义记忆
        val definitionMemories = memories.filter { it.type == MemoryType.SYMBOL_DEFINITION }
        assertTrue(definitionMemories.isNotEmpty())

        val classDefinitionMemory = definitionMemories.find { it.sourceElements.any { element -> element.id == "class1" } }
        assertTrue(classDefinitionMemory != null)
        assertTrue(classDefinitionMemory!!.content.contains("TestClass"))
        assertTrue(classDefinitionMemory.content.contains("文档: This is a test class."))
    }

    @Test
    fun `test generating memories from symbol node`() = runBlocking {
        // 创建符号节点
        val classSymbol = SymbolNode(
            id = "symbol1",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = SymbolType.CLASS,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Paths.get("TestClass.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 20,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )

        val interfaceSymbol = SymbolNode(
            id = "symbol2",
            name = "TestInterface",
            qualifiedName = "com.example.TestInterface",
            type = SymbolType.INTERFACE,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Paths.get("TestInterface.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )

        val methodSymbol = SymbolNode(
            id = "symbol3",
            name = "testMethod",
            qualifiedName = "com.example.TestClass.testMethod",
            type = SymbolType.METHOD,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Paths.get("TestClass.java").toString(),
                startLine = 5,
                startColumn = 5,
                endLine = 8,
                endColumn = 5
            ),
            visibility = Visibility.PUBLIC
        )

        // 添加符号到图
        symbolGraph.addNode(classSymbol)
        symbolGraph.addNode(interfaceSymbol)
        symbolGraph.addNode(methodSymbol)

        // 添加关系
        symbolGraph.addRelation(
            ai.kastrax.codebase.symbol.model.SymbolRelation(
                sourceId = classSymbol.id,
                targetId = interfaceSymbol.id,
                type = SymbolRelationType.IMPLEMENTS
            )
        )

        symbolGraph.addRelation(
            ai.kastrax.codebase.symbol.model.SymbolRelation(
                sourceId = methodSymbol.id,
                targetId = interfaceSymbol.id,
                type = SymbolRelationType.REFERENCES
            )
        )

        // 生成记忆
        val memories = memoryGenerator.generateMemoriesFromSymbolNode(classSymbol, symbolGraph)

        // 验证记忆
        assertTrue(memories.isNotEmpty())

        // 验证符号定义记忆
        val definitionMemories = memories.filter { it.type == MemoryType.SYMBOL_DEFINITION }
        assertTrue(definitionMemories.isNotEmpty())

        val classDefinitionMemory = definitionMemories.find { it.sourceSymbols.any { symbol -> symbol.id == "symbol1" } }
        assertTrue(classDefinitionMemory != null)
        assertTrue(classDefinitionMemory!!.content.contains("TestClass"))

        // 验证实现记忆
        val implementationMemories = memories.filter { it.type == MemoryType.IMPLEMENTATION }
        assertTrue(implementationMemories.isNotEmpty())

        val implementsMemory = implementationMemories.find {
            it.sourceSymbols.any { symbol -> symbol.id == "symbol1" } &&
            it.sourceSymbols.any { symbol -> symbol.id == "symbol2" }
        }
        assertTrue(implementsMemory != null)
        assertTrue(implementsMemory!!.content.contains("TestClass"))
        assertTrue(implementsMemory.content.contains("实现了以下接口"))
        assertTrue(implementsMemory.content.contains("TestInterface"))
    }

    @Test
    fun `test generating custom memory`() {
        // 创建自定义记忆
        val memory = memoryGenerator.generateCustomMemory(
            content = "This is a custom memory",
            importance = ImportanceLevel.HIGH,
            metadata = mapOf("key1" to "value1", "key2" to "value2")
        )

        // 验证记忆
        assertEquals(MemoryType.CUSTOM, memory.type)
        assertEquals("This is a custom memory", memory.content)
        assertEquals(ImportanceLevel.HIGH, memory.importance)
        assertEquals(2, memory.metadata.size)
        assertEquals("value1", memory.metadata["key1"])
        assertEquals("value2", memory.metadata["key2"])
    }

    @Test
    fun `test generating semantic relation memory`() {
        // 创建符号节点
        val classSymbol = SymbolNode(
            id = "symbol1",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = SymbolType.CLASS,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Paths.get("TestClass.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 20,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )

        val interfaceSymbol = SymbolNode(
            id = "symbol2",
            name = "TestInterface",
            qualifiedName = "com.example.TestInterface",
            type = SymbolType.INTERFACE,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Paths.get("TestInterface.java").toString(),
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )

        // 生成语义关系记忆
        val memory = memoryGenerator.generateSemanticRelationMemory(
            sourceNode = classSymbol,
            targetNode = interfaceSymbol,
            relationType = SymbolRelationType.IMPLEMENTS,
            description = "实现了接口"
        )

        // 验证记忆
        assertEquals(MemoryType.SEMANTIC_RELATION, memory.type)
        assertTrue(memory.content.contains("TestClass"))
        assertTrue(memory.content.contains("实现了接口"))
        assertTrue(memory.content.contains("TestInterface"))
        assertEquals(2, memory.sourceSymbols.size)
        assertTrue(memory.sourceSymbols.any { it.id == "symbol1" })
        assertTrue(memory.sourceSymbols.any { it.id == "symbol2" })
        assertEquals(SymbolRelationType.IMPLEMENTS.name, memory.metadata["relationType"])
    }
}
