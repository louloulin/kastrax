package ai.kastrax.core.agent.version

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Agent版本管理器测试类
 */
class AgentVersionManagerTest {

    private lateinit var versionManager: AgentVersionManager
    private val testAgentId = "test-agent"
    private val initialInstructions = "You are a helpful assistant."
    private val updatedInstructions = "You are a helpful and friendly assistant."

    @BeforeEach
    fun setUp() {
        versionManager = InMemoryAgentVersionManager()
    }

    @Test
    fun `test create version`() = runBlocking {
        // 创建版本
        val version = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            name = "Test Version",
            description = "Test description",
            activateImmediately = true
        )

        // 验证版本信息
        assertNotNull(version)
        assertEquals(testAgentId, version.agentId)
        assertEquals(1, version.versionNumber)
        assertEquals("Test Version", version.name)
        assertEquals(initialInstructions, version.instructions)
        assertEquals("Test description", version.description)
        assertEquals(AgentVersionStatus.ACTIVE, version.status)
    }

    @Test
    fun `test get version`() = runBlocking {
        // 创建版本
        val createdVersion = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions
        )

        // 获取版本
        val retrievedVersion = versionManager.getVersion(createdVersion.id)

        // 验证版本信息
        assertNotNull(retrievedVersion)
        assertEquals(createdVersion.id, retrievedVersion?.id)
        assertEquals(testAgentId, retrievedVersion?.agentId)
        assertEquals(initialInstructions, retrievedVersion?.instructions)
    }

    @Test
    fun `test get versions`() = runBlocking {
        // 创建多个版本
        val version1 = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            name = "Version 1"
        )

        val version2 = versionManager.createVersion(
            agentId = testAgentId,
            instructions = updatedInstructions,
            name = "Version 2"
        )

        // 获取所有版本
        val versions = versionManager.getVersions(testAgentId)

        // 验证版本列表
        assertEquals(2, versions.size)
        assertEquals(2, versions[0].versionNumber) // 按版本号降序排序
        assertEquals(1, versions[1].versionNumber)
    }

    @Test
    fun `test get active version`() = runBlocking {
        // 创建非激活版本
        versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            activateImmediately = false
        )

        // 获取激活版本（应为null）
        val activeVersion1 = versionManager.getActiveVersion(testAgentId)
        assertNull(activeVersion1)

        // 创建激活版本
        val activatedVersion = versionManager.createVersion(
            agentId = testAgentId,
            instructions = updatedInstructions,
            activateImmediately = true
        )

        // 获取激活版本
        val activeVersion2 = versionManager.getActiveVersion(testAgentId)
        assertNotNull(activeVersion2)
        assertEquals(activatedVersion.id, activeVersion2?.id)
        assertEquals(AgentVersionStatus.ACTIVE, activeVersion2?.status)
    }

    @Test
    fun `test activate version`() = runBlocking {
        // 创建非激活版本
        val version = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            activateImmediately = false
        )

        // 验证初始状态
        assertEquals(AgentVersionStatus.DRAFT, version.status)
        assertNull(versionManager.getActiveVersion(testAgentId))

        // 激活版本
        val activatedVersion = versionManager.activateVersion(version.id)

        // 验证激活后状态
        assertNotNull(activatedVersion)
        assertEquals(AgentVersionStatus.ACTIVE, activatedVersion?.status)
        
        // 验证通过getActiveVersion获取
        val activeVersion = versionManager.getActiveVersion(testAgentId)
        assertNotNull(activeVersion)
        assertEquals(version.id, activeVersion?.id)
    }

    @Test
    fun `test rollback to version`() = runBlocking {
        // 创建两个版本
        val version1 = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            activateImmediately = true
        )

        val version2 = versionManager.createVersion(
            agentId = testAgentId,
            instructions = updatedInstructions,
            activateImmediately = true
        )

        // 验证当前激活版本是version2
        val activeVersion1 = versionManager.getActiveVersion(testAgentId)
        assertEquals(version2.id, activeVersion1?.id)

        // 回滚到version1
        val rolledBackVersion = versionManager.rollbackToVersion(version1.id)

        // 验证回滚后激活版本是version1
        assertNotNull(rolledBackVersion)
        assertEquals(version1.id, rolledBackVersion?.id)
        assertEquals(AgentVersionStatus.ACTIVE, rolledBackVersion?.status)
        
        // 验证通过getActiveVersion获取
        val activeVersion2 = versionManager.getActiveVersion(testAgentId)
        assertEquals(version1.id, activeVersion2?.id)
        
        // 验证version2状态变为ARCHIVED
        val version2Updated = versionManager.getVersion(version2.id)
        assertEquals(AgentVersionStatus.ARCHIVED, version2Updated?.status)
    }

    @Test
    fun `test update version`() = runBlocking {
        // 创建版本
        val version = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            name = "Original Name",
            description = "Original description"
        )

        // 更新版本
        val updates = mapOf(
            "name" to "Updated Name",
            "description" to "Updated description",
            "status" to AgentVersionStatus.DEPRECATED.name
        )
        
        val updatedVersion = versionManager.updateVersion(version.id, updates)

        // 验证更新后的版本
        assertEquals("Updated Name", updatedVersion.name)
        assertEquals("Updated description", updatedVersion.description)
        assertEquals(AgentVersionStatus.DEPRECATED, updatedVersion.status)
    }

    @Test
    fun `test delete version`() = runBlocking {
        // 创建非激活版本
        val version = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            activateImmediately = false
        )

        // 删除版本
        val deleted = versionManager.deleteVersion(version.id)
        assertTrue(deleted)

        // 验证版本已删除
        val retrievedVersion = versionManager.getVersion(version.id)
        assertNull(retrievedVersion)
    }

    @Test
    fun `test cannot delete active version`() = runBlocking {
        // 创建激活版本
        val version = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            activateImmediately = true
        )

        // 尝试删除激活版本，应抛出异常
        val exception = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                versionManager.deleteVersion(version.id)
            }
        }
        
        assertTrue(exception.message?.contains("不能删除激活的版本") ?: false)
        
        // 验证版本未删除
        val retrievedVersion = versionManager.getVersion(version.id)
        assertNotNull(retrievedVersion)
    }

    @Test
    fun `test compare versions`() = runBlocking {
        // 创建两个版本
        val version1 = versionManager.createVersion(
            agentId = testAgentId,
            instructions = initialInstructions,
            metadata = mapOf("key1" to "value1", "key2" to "value2")
        )

        val version2 = versionManager.createVersion(
            agentId = testAgentId,
            instructions = updatedInstructions,
            metadata = mapOf("key1" to "value1", "key3" to "value3")
        )

        // 比较版本
        val diff = versionManager.compareVersions(version1.id, version2.id)

        // 验证差异
        assertEquals(version1.id, diff.versionId1)
        assertEquals(version2.id, diff.versionId2)
        assertTrue(diff.instructionsDiff.contains("-") && diff.instructionsDiff.contains("+"))
        assertEquals(2, diff.metadataDiff.size)
        assertTrue(diff.metadataDiff.containsKey("key2"))
        assertTrue(diff.metadataDiff.containsKey("key3"))
    }
}
