package actor.proto.pidset

import actor.proto.PID
import java.util.concurrent.ConcurrentHashMap

/**
 * PIDSet 是一个高效的 PID 集合实现，用于存储和操作 PID 集合
 */
class PIDSet {
    private val pids = ConcurrentHashMap.newKeySet<PID>()
    
    /**
     * 添加 PID 到集合
     * @param pid 要添加的 PID
     * @return 如果集合被修改则返回 true，否则返回 false
     */
    fun add(pid: PID): Boolean = pids.add(pid)
    
    /**
     * 添加多个 PID 到集合
     * @param pids 要添加的 PID 集合
     * @return 如果集合被修改则返回 true，否则返回 false
     */
    fun addAll(pids: Collection<PID>): Boolean = this.pids.addAll(pids)
    
    /**
     * 从集合中移除 PID
     * @param pid 要移除的 PID
     * @return 如果集合被修改则返回 true，否则返回 false
     */
    fun remove(pid: PID): Boolean = pids.remove(pid)
    
    /**
     * 从集合中移除多个 PID
     * @param pids 要移除的 PID 集合
     * @return 如果集合被修改则返回 true，否则返回 false
     */
    fun removeAll(pids: Collection<PID>): Boolean = this.pids.removeAll(pids)
    
    /**
     * 检查集合是否包含指定的 PID
     * @param pid 要检查的 PID
     * @return 如果集合包含指定的 PID 则返回 true，否则返回 false
     */
    fun contains(pid: PID): Boolean = pids.contains(pid)
    
    /**
     * 获取集合中的 PID 数量
     * @return 集合中的 PID 数量
     */
    fun count(): Int = pids.size
    
    /**
     * 检查集合是否为空
     * @return 如果集合为空则返回 true，否则返回 false
     */
    fun isEmpty(): Boolean = pids.isEmpty()
    
    /**
     * 清空集合
     */
    fun clear() = pids.clear()
    
    /**
     * 获取集合中的所有 PID
     * @return 集合中的所有 PID
     */
    fun toSet(): Set<PID> = pids.toSet()
    
    /**
     * 对集合中的每个 PID 执行指定的操作
     * @param action 要执行的操作
     */
    fun forEach(action: (PID) -> Unit) = pids.forEach(action)
    
    /**
     * 创建一个新的 PIDSet，包含当前集合和指定集合的并集
     * @param other 要合并的 PID 集合
     * @return 包含两个集合并集的新 PIDSet
     */
    fun union(other: PIDSet): PIDSet {
        val result = PIDSet()
        result.addAll(this.pids)
        result.addAll(other.pids)
        return result
    }
    
    /**
     * 创建一个新的 PIDSet，包含当前集合和指定集合的交集
     * @param other 要计算交集的 PID 集合
     * @return 包含两个集合交集的新 PIDSet
     */
    fun intersect(other: PIDSet): PIDSet {
        val result = PIDSet()
        for (pid in this.pids) {
            if (other.contains(pid)) {
                result.add(pid)
            }
        }
        return result
    }
    
    /**
     * 创建一个新的 PIDSet，包含当前集合中不在指定集合中的元素
     * @param other 要排除的 PID 集合
     * @return 包含当前集合减去指定集合的新 PIDSet
     */
    fun except(other: PIDSet): PIDSet {
        val result = PIDSet()
        for (pid in this.pids) {
            if (!other.contains(pid)) {
                result.add(pid)
            }
        }
        return result
    }
    
    /**
     * 检查当前集合是否是指定集合的子集
     * @param other 要检查的 PID 集合
     * @return 如果当前集合是指定集合的子集则返回 true，否则返回 false
     */
    fun isSubsetOf(other: PIDSet): Boolean {
        for (pid in this.pids) {
            if (!other.contains(pid)) {
                return false
            }
        }
        return true
    }
    
    /**
     * 检查当前集合是否是指定集合的超集
     * @param other 要检查的 PID 集合
     * @return 如果当前集合是指定集合的超集则返回 true，否则返回 false
     */
    fun isSupersetOf(other: PIDSet): Boolean {
        return other.isSubsetOf(this)
    }
    
    /**
     * 检查当前集合是否与指定集合相等
     * @param other 要检查的 PID 集合
     * @return 如果当前集合与指定集合相等则返回 true，否则返回 false
     */
    fun equals(other: PIDSet): Boolean {
        if (this.count() != other.count()) {
            return false
        }
        return this.isSubsetOf(other)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PIDSet) return false
        return equals(other)
    }
    
    override fun hashCode(): Int {
        return pids.hashCode()
    }
    
    override fun toString(): String {
        return "PIDSet(count=${count()})"
    }
    
    companion object {
        /**
         * 创建一个空的 PIDSet
         * @return 空的 PIDSet
         */
        fun empty(): PIDSet = PIDSet()
        
        /**
         * 从 PID 集合创建 PIDSet
         * @param pids 要添加的 PID 集合
         * @return 包含指定 PID 的 PIDSet
         */
        fun from(pids: Collection<PID>): PIDSet {
            val result = PIDSet()
            result.addAll(pids)
            return result
        }
        
        /**
         * 从 PID 数组创建 PIDSet
         * @param pids 要添加的 PID 数组
         * @return 包含指定 PID 的 PIDSet
         */
        fun of(vararg pids: PID): PIDSet {
            val result = PIDSet()
            for (pid in pids) {
                result.add(pid)
            }
            return result
        }
    }
}
