package actor.proto.pidset

import actor.proto.PID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class PIDSetTest {
    private lateinit var pidSet: PIDSet
    private lateinit var pid1: PID
    private lateinit var pid2: PID
    private lateinit var pid3: PID
    
    @BeforeEach
    fun setup() {
        pidSet = PIDSet()
        pid1 = PID("host1", "id1")
        pid2 = PID("host2", "id2")
        pid3 = PID("host3", "id3")
    }
    
    @Test
    fun `should add PID to set`() {
        assertTrue(pidSet.isEmpty())
        
        val result = pidSet.add(pid1)
        
        assertTrue(result)
        assertFalse(pidSet.isEmpty())
        assertEquals(1, pidSet.count())
        assertTrue(pidSet.contains(pid1))
    }
    
    @Test
    fun `should not add duplicate PID to set`() {
        pidSet.add(pid1)
        
        val result = pidSet.add(pid1)
        
        assertFalse(result)
        assertEquals(1, pidSet.count())
    }
    
    @Test
    fun `should add multiple PIDs to set`() {
        val result = pidSet.addAll(listOf(pid1, pid2, pid3))
        
        assertTrue(result)
        assertEquals(3, pidSet.count())
        assertTrue(pidSet.contains(pid1))
        assertTrue(pidSet.contains(pid2))
        assertTrue(pidSet.contains(pid3))
    }
    
    @Test
    fun `should remove PID from set`() {
        pidSet.add(pid1)
        
        val result = pidSet.remove(pid1)
        
        assertTrue(result)
        assertTrue(pidSet.isEmpty())
    }
    
    @Test
    fun `should not remove non-existent PID from set`() {
        val result = pidSet.remove(pid1)
        
        assertFalse(result)
    }
    
    @Test
    fun `should remove multiple PIDs from set`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))
        
        val result = pidSet.removeAll(listOf(pid1, pid2))
        
        assertTrue(result)
        assertEquals(1, pidSet.count())
        assertFalse(pidSet.contains(pid1))
        assertFalse(pidSet.contains(pid2))
        assertTrue(pidSet.contains(pid3))
    }
    
    @Test
    fun `should clear set`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))
        
        pidSet.clear()
        
        assertTrue(pidSet.isEmpty())
    }
    
    @Test
    fun `should convert set to Set`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))
        
        val set = pidSet.toSet()
        
        assertEquals(3, set.size)
        assertTrue(set.contains(pid1))
        assertTrue(set.contains(pid2))
        assertTrue(set.contains(pid3))
    }
    
    @Test
    fun `should iterate over set`() {
        pidSet.addAll(listOf(pid1, pid2, pid3))
        
        val pids = mutableListOf<PID>()
        pidSet.forEach { pids.add(it) }
        
        assertEquals(3, pids.size)
        assertTrue(pids.contains(pid1))
        assertTrue(pids.contains(pid2))
        assertTrue(pids.contains(pid3))
    }
    
    @Test
    fun `should create union of sets`() {
        val set1 = PIDSet.of(pid1, pid2)
        val set2 = PIDSet.of(pid2, pid3)
        
        val union = set1.union(set2)
        
        assertEquals(3, union.count())
        assertTrue(union.contains(pid1))
        assertTrue(union.contains(pid2))
        assertTrue(union.contains(pid3))
    }
    
    @Test
    fun `should create intersection of sets`() {
        val set1 = PIDSet.of(pid1, pid2)
        val set2 = PIDSet.of(pid2, pid3)
        
        val intersection = set1.intersect(set2)
        
        assertEquals(1, intersection.count())
        assertFalse(intersection.contains(pid1))
        assertTrue(intersection.contains(pid2))
        assertFalse(intersection.contains(pid3))
    }
    
    @Test
    fun `should create except of sets`() {
        val set1 = PIDSet.of(pid1, pid2)
        val set2 = PIDSet.of(pid2, pid3)
        
        val except = set1.except(set2)
        
        assertEquals(1, except.count())
        assertTrue(except.contains(pid1))
        assertFalse(except.contains(pid2))
        assertFalse(except.contains(pid3))
    }
    
    @Test
    fun `should check if set is subset of another set`() {
        val set1 = PIDSet.of(pid1, pid2)
        val set2 = PIDSet.of(pid1, pid2, pid3)
        
        assertTrue(set1.isSubsetOf(set2))
        assertFalse(set2.isSubsetOf(set1))
    }
    
    @Test
    fun `should check if set is superset of another set`() {
        val set1 = PIDSet.of(pid1, pid2)
        val set2 = PIDSet.of(pid1, pid2, pid3)
        
        assertFalse(set1.isSupersetOf(set2))
        assertTrue(set2.isSupersetOf(set1))
    }
    
    @Test
    fun `should check if sets are equal`() {
        val set1 = PIDSet.of(pid1, pid2)
        val set2 = PIDSet.of(pid1, pid2)
        val set3 = PIDSet.of(pid1, pid2, pid3)
        
        assertTrue(set1.equals(set2))
        assertFalse(set1.equals(set3))
    }
    
    @Test
    fun `should create empty set`() {
        val emptySet = PIDSet.empty()
        
        assertTrue(emptySet.isEmpty())
    }
    
    @Test
    fun `should create set from collection`() {
        val set = PIDSet.from(listOf(pid1, pid2, pid3))
        
        assertEquals(3, set.count())
        assertTrue(set.contains(pid1))
        assertTrue(set.contains(pid2))
        assertTrue(set.contains(pid3))
    }
    
    @Test
    fun `should create set from varargs`() {
        val set = PIDSet.of(pid1, pid2, pid3)
        
        assertEquals(3, set.count())
        assertTrue(set.contains(pid1))
        assertTrue(set.contains(pid2))
        assertTrue(set.contains(pid3))
    }
}
