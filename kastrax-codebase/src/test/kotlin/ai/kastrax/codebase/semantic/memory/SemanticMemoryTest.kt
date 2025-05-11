package ai.kastrax.codebase.semantic.memory

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolType
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SemanticMemoryTest {
    
    @Test
    fun `test creating and accessing semantic memory`() {
        // 创建语义记忆
        val memory = SemanticMemory(
            type = MemoryType.CODE_STRUCTURE,
            content = "This is a test memory",
            importance = ImportanceLevel.HIGH
        )
        
        // 验证记忆属性
        assertEquals(MemoryType.CODE_STRUCTURE, memory.type)
        assertEquals("This is a test memory", memory.content)
        assertEquals(ImportanceLevel.HIGH, memory.importance)
        assertEquals(0, memory.accessCount)
        
        // 访问记忆
        memory.access()
        
        // 验证访问计数
        assertEquals(1, memory.accessCount)
        
        // 再次访问
        memory.access()
        
        // 验证访问计数
        assertEquals(2, memory.accessCount)
    }
    
    @Test
    fun `test adding and removing related memories`() {
        // 创建语义记忆
        val memory = SemanticMemory(
            type = MemoryType.CODE_STRUCTURE,
            content = "This is a test memory"
        )
        
        // 添加相关记忆
        memory.addRelatedMemory("memory1")
        memory.addRelatedMemory("memory2")
        
        // 验证相关记忆
        assertEquals(2, memory.relatedMemories.size)
        assertTrue(memory.relatedMemories.contains("memory1"))
        assertTrue(memory.relatedMemories.contains("memory2"))
        
        // 添加重复的相关记忆
        memory.addRelatedMemory("memory1")
        
        // 验证相关记忆数量没有变化
        assertEquals(2, memory.relatedMemories.size)
        
        // 移除相关记忆
        val removed = memory.removeRelatedMemory("memory1")
        
        // 验证移除结果
        assertTrue(removed)
        assertEquals(1, memory.relatedMemories.size)
        assertFalse(memory.relatedMemories.contains("memory1"))
        assertTrue(memory.relatedMemories.contains("memory2"))
        
        // 移除不存在的相关记忆
        val notRemoved = memory.removeRelatedMemory("memory3")
        
        // 验证移除结果
        assertFalse(notRemoved)
        assertEquals(1, memory.relatedMemories.size)
    }
    
    @Test
    fun `test memory store operations`() {
        // 创建记忆存储
        val store = SemanticMemoryStore("test-store")
        
        // 创建语义记忆
        val memory1 = SemanticMemory(
            id = "memory1",
            type = MemoryType.CODE_STRUCTURE,
            content = "This is memory 1",
            importance = ImportanceLevel.HIGH
        )
        
        val memory2 = SemanticMemory(
            id = "memory2",
            type = MemoryType.SYMBOL_DEFINITION,
            content = "This is memory 2",
            importance = ImportanceLevel.MEDIUM
        )
        
        // 添加记忆
        val added1 = store.addMemory(memory1)
        val added2 = store.addMemory(memory2)
        
        // 验证添加结果
        assertTrue(added1)
        assertTrue(added2)
        
        // 获取记忆
        val retrievedMemory1 = store.getMemory("memory1")
        val retrievedMemory2 = store.getMemory("memory2")
        
        // 验证获取结果
        assertNotNull(retrievedMemory1)
        assertNotNull(retrievedMemory2)
        assertEquals("memory1", retrievedMemory1.id)
        assertEquals("memory2", retrievedMemory2.id)
        
        // 获取所有记忆
        val allMemories = store.getAllMemories()
        
        // 验证所有记忆
        assertEquals(2, allMemories.size)
        assertTrue(allMemories.any { it.id == "memory1" })
        assertTrue(allMemories.any { it.id == "memory2" })
        
        // 根据类型查找记忆
        val codeStructureMemories = store.findMemoriesByType(MemoryType.CODE_STRUCTURE)
        val symbolDefinitionMemories = store.findMemoriesByType(MemoryType.SYMBOL_DEFINITION)
        
        // 验证查找结果
        assertEquals(1, codeStructureMemories.size)
        assertEquals(1, symbolDefinitionMemories.size)
        assertEquals("memory1", codeStructureMemories.first().id)
        assertEquals("memory2", symbolDefinitionMemories.first().id)
        
        // 根据重要性查找记忆
        val highImportanceMemories = store.findMemoriesByImportance(ImportanceLevel.HIGH)
        val mediumImportanceMemories = store.findMemoriesByImportance(ImportanceLevel.MEDIUM)
        
        // 验证查找结果
        assertEquals(1, highImportanceMemories.size)
        assertEquals(1, mediumImportanceMemories.size)
        assertEquals("memory1", highImportanceMemories.first().id)
        assertEquals("memory2", mediumImportanceMemories.first().id)
        
        // 更新记忆
        val updatedMemory1 = memory1.copy(content = "This is updated memory 1")
        val updated = store.updateMemory(updatedMemory1)
        
        // 验证更新结果
        assertTrue(updated)
        
        // 获取更新后的记忆
        val retrievedUpdatedMemory1 = store.getMemory("memory1")
        
        // 验证更新后的记忆
        assertNotNull(retrievedUpdatedMemory1)
        assertEquals("This is updated memory 1", retrievedUpdatedMemory1.content)
        
        // 添加相关记忆
        memory1.addRelatedMemory("memory2")
        store.updateMemory(memory1)
        
        // 查找相关记忆
        val relatedMemories = store.findRelatedMemories("memory1")
        
        // 验证相关记忆
        assertEquals(1, relatedMemories.size)
        assertEquals("memory2", relatedMemories.first().id)
        
        // 移除记忆
        val removed = store.removeMemory("memory1")
        
        // 验证移除结果
        assertTrue(removed)
        
        // 获取移除后的记忆
        val retrievedRemovedMemory1 = store.getMemory("memory1")
        
        // 验证移除后的记忆
        assertEquals(null, retrievedRemovedMemory1)
        
        // 获取所有记忆
        val remainingMemories = store.getAllMemories()
        
        // 验证剩余记忆
        assertEquals(1, remainingMemories.size)
        assertEquals("memory2", remainingMemories.first().id)
    }
    
    @Test
    fun `test memory store with code elements and symbols`() {
        // 创建记忆存储
        val store = SemanticMemoryStore("test-store")
        
        // 创建代码元素
        val element1 = CodeElement(
            id = "element1",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = Path("TestClass.java"),
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )
        
        val element2 = CodeElement(
            id = "element2",
            name = "testMethod",
            qualifiedName = "com.example.TestClass.testMethod",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = Path("TestClass.java"),
                startLine = 5,
                startColumn = 5,
                endLine = 8,
                endColumn = 5
            ),
            visibility = Visibility.PUBLIC,
            parent = element1
        )
        
        // 创建符号节点
        val symbol1 = SymbolNode(
            id = "symbol1",
            name = "TestClass",
            qualifiedName = "com.example.TestClass",
            type = SymbolType.CLASS,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Path("TestClass.java"),
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC
        )
        
        val symbol2 = SymbolNode(
            id = "symbol2",
            name = "testMethod",
            qualifiedName = "com.example.TestClass.testMethod",
            type = SymbolType.METHOD,
            kind = ai.kastrax.codebase.symbol.model.SymbolKind.DEFINITION,
            location = Location(
                filePath = Path("TestClass.java"),
                startLine = 5,
                startColumn = 5,
                endLine = 8,
                endColumn = 5
            ),
            visibility = Visibility.PUBLIC
        )
        
        // 创建语义记忆
        val memory1 = SemanticMemory(
            id = "memory1",
            type = MemoryType.CODE_STRUCTURE,
            content = "Class TestClass",
            sourceElements = listOf(element1),
            importance = ImportanceLevel.HIGH
        )
        
        val memory2 = SemanticMemory(
            id = "memory2",
            type = MemoryType.SYMBOL_DEFINITION,
            content = "Method testMethod in class TestClass",
            sourceElements = listOf(element2),
            sourceSymbols = listOf(symbol2),
            importance = ImportanceLevel.MEDIUM
        )
        
        // 添加记忆
        store.addMemory(memory1)
        store.addMemory(memory2)
        
        // 根据代码元素查找记忆
        val elementMemories1 = store.findMemoriesByElement("element1")
        val elementMemories2 = store.findMemoriesByElement("element2")
        
        // 验证查找结果
        assertEquals(1, elementMemories1.size)
        assertEquals(1, elementMemories2.size)
        assertEquals("memory1", elementMemories1.first().id)
        assertEquals("memory2", elementMemories2.first().id)
        
        // 根据符号查找记忆
        val symbolMemories2 = store.findMemoriesBySymbol("symbol2")
        
        // 验证查找结果
        assertEquals(1, symbolMemories2.size)
        assertEquals("memory2", symbolMemories2.first().id)
        
        // 根据文件路径查找记忆
        val fileMemories = store.findMemoriesByFile(Path("TestClass.java"))
        
        // 验证查找结果
        assertEquals(2, fileMemories.size)
        assertTrue(fileMemories.any { it.id == "memory1" })
        assertTrue(fileMemories.any { it.id == "memory2" })
    }
}
