package actor.proto.cluster.consensus

import actor.proto.cluster.GossipMemberState
import actor.proto.cluster.MemberGossipState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class ConsensusCheckBuilderTest {
    private lateinit var builder: ConsensusCheckBuilder

    @BeforeEach
    fun setup() {
        builder = ConsensusCheckBuilder("test.key") { value ->
            when (value) {
                is String -> value
                is Int -> value.toString()
                else -> null
            }
        }
    }

    @Test
    fun `should build consensus check`() {
        val (consensus, check) = builder.build()

        assertNotNull(consensus)
        assertEquals("test.key", consensus.getId())

        assertNotNull(check)
    }

    @Test
    fun `should check consensus with same values`() {
        val checker = builder.check()

        // 创建具有相同值的八卦状态
        val state = MemberGossipState(
            mapOf(
                "member1" to GossipMemberState("member1", mapOf("test.key" to "value")),
                "member2" to GossipMemberState("member2", mapOf("test.key" to "value")),
                "member3" to GossipMemberState("member3", mapOf("test.key" to "value"))
            )
        )

        val members = setOf("member1", "member2", "member3")

        // 检查共识
        val (hasConsensus, consensusValue) = checker(state, members)

        assertTrue(hasConsensus)
        assertEquals("value", consensusValue)
    }

    @Test
    fun `should not have consensus with different values`() {
        val checker = builder.check()

        // 创建具有不同值的八卦状态
        val state = MemberGossipState(
            mapOf(
                "member1" to GossipMemberState("member1", mapOf("test.key" to "value1")),
                "member2" to GossipMemberState("member2", mapOf("test.key" to "value2")),
                "member3" to GossipMemberState("member3", mapOf("test.key" to "value3"))
            )
        )

        val members = setOf("member1", "member2", "member3")

        // 检查共识
        val (hasConsensus, consensusValue) = checker(state, members)

        assertFalse(hasConsensus)
        assertNull(consensusValue)
    }

    @Test
    fun `should not have consensus with missing members`() {
        val checker = builder.check()

        // 创建缺少成员的八卦状态
        val state = MemberGossipState(
            mapOf(
                "member1" to GossipMemberState("member1", mapOf("test.key" to "value")),
                "member2" to GossipMemberState("member2", mapOf("test.key" to "value"))
            )
        )

        val members = setOf("member1", "member2", "member3")

        // 检查共识
        val (hasConsensus, consensusValue) = checker(state, members)

        assertFalse(hasConsensus)
        assertNull(consensusValue)
    }

    @Test
    fun `should not have consensus with missing values`() {
        val checker = builder.check()

        // 创建缺少值的八卦状态
        val state = MemberGossipState(
            mapOf(
                "member1" to GossipMemberState("member1", mapOf("test.key" to "value")),
                "member2" to GossipMemberState("member2", emptyMap()),
                "member3" to GossipMemberState("member3", mapOf("test.key" to "value"))
            )
        )

        val members = setOf("member1", "member2", "member3")

        // 检查共识
        val (hasConsensus, consensusValue) = checker(state, members)

        assertFalse(hasConsensus)
        assertNull(consensusValue)
    }
}

class ConsensusChecksTest {
    private lateinit var checks: ConsensusChecks

    @BeforeEach
    fun setup() {
        checks = ConsensusChecks()
    }

    @Test
    fun `should add and get consensus check`() {
        val key = "test-key"
        val check = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        }

        // 添加共识检查
        checks.add(key, check)

        // 获取共识检查
        val retrievedCheck = checks.get(key)
        assertNotNull(retrievedCheck)
        assertSame(check, retrievedCheck)
    }

    @Test
    fun `should get affected checks`() {
        val key1 = "test-key-1"
        val key2 = "test-key-2"
        val check1 = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        }
        val check2 = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        }

        // 添加共识检查
        checks.add(key1, check1)
        checks.add(key2, check2)

        // 获取受影响的检查
        val affectedChecks1 = checks.getAffectedChecks("test")
        assertEquals(2, affectedChecks1.size)
        assertTrue(affectedChecks1.contains(key1))
        assertTrue(affectedChecks1.contains(key2))

        val affectedChecks2 = checks.getAffectedChecks("key1")
        assertEquals(1, affectedChecks2.size)
        assertTrue(affectedChecks2.contains(key1))

        val affectedChecks3 = checks.getAffectedChecks("key2")
        assertEquals(1, affectedChecks3.size)
        assertTrue(affectedChecks3.contains(key2))
    }

    @Test
    fun `should remove consensus check`() {
        val key = "test-key"
        val check = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        }

        // 添加共识检查
        checks.add(key, check)

        // 移除共识检查
        val removeResult = checks.remove(key)
        assertTrue(removeResult)

        // 获取共识检查，应该返回 null
        val retrievedCheck = checks.get(key)
        assertNull(retrievedCheck)

        // 获取受影响的检查，应该为空
        val affectedChecks = checks.getAffectedChecks("test")
        assertTrue(affectedChecks.isEmpty())
    }

    @Test
    fun `should clear all consensus checks`() {
        // 添加多个共识检查
        checks.add("key1", object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        })
        checks.add("key2", object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        })
        checks.add("key3", object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return null
            }
        })

        // 清除所有共识检查
        checks.clear()

        // 获取共识检查，应该返回 null
        assertNull(checks.get("key1"))
        assertNull(checks.get("key2"))
        assertNull(checks.get("key3"))

        // 获取受影响的检查，应该为空
        assertTrue(checks.getAffectedChecks("test1").isEmpty())
        assertTrue(checks.getAffectedChecks("test2").isEmpty())
        assertTrue(checks.getAffectedChecks("test3").isEmpty())
    }
}
