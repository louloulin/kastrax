package actor.proto.pidset

import actor.proto.PID

/**
 * 将 PID 集合转换为 PIDSet
 * @return 包含集合中所有 PID 的 PIDSet
 */
fun Collection<PID>.toPIDSet(): PIDSet = PIDSet.from(this)

/**
 * 将 PID 数组转换为 PIDSet
 * @return 包含数组中所有 PID 的 PIDSet
 */
fun Array<PID>.toPIDSet(): PIDSet = PIDSet.from(this.toList())

/**
 * 创建一个新的 PIDSet，包含当前集合和指定 PID 的并集
 * @param pid 要添加的 PID
 * @return 包含当前集合和指定 PID 的新 PIDSet
 */
operator fun PIDSet.plus(pid: PID): PIDSet {
    val result = PIDSet()
    result.addAll(this.toSet())
    result.add(pid)
    return result
}

/**
 * 创建一个新的 PIDSet，包含当前集合和指定集合的并集
 * @param other 要合并的 PID 集合
 * @return 包含两个集合并集的新 PIDSet
 */
operator fun PIDSet.plus(other: PIDSet): PIDSet = this.union(other)

/**
 * 创建一个新的 PIDSet，包含当前集合中不包含指定 PID 的元素
 * @param pid 要移除的 PID
 * @return 包含当前集合减去指定 PID 的新 PIDSet
 */
operator fun PIDSet.minus(pid: PID): PIDSet {
    val result = PIDSet()
    result.addAll(this.toSet())
    result.remove(pid)
    return result
}

/**
 * 创建一个新的 PIDSet，包含当前集合中不在指定集合中的元素
 * @param other 要排除的 PID 集合
 * @return 包含当前集合减去指定集合的新 PIDSet
 */
operator fun PIDSet.minus(other: PIDSet): PIDSet = this.except(other)

/**
 * 检查 PID 是否在集合中
 * @param pid 要检查的 PID
 * @return 如果 PID 在集合中则返回 true，否则返回 false
 */
operator fun PIDSet.contains(pid: PID): Boolean = this.contains(pid)

/**
 * 获取集合的大小
 * @return 集合中的 PID 数量
 */
val PIDSet.size: Int
    get() = this.count()

/**
 * 获取集合的迭代器
 * @return 集合的迭代器
 */
operator fun PIDSet.iterator(): Iterator<PID> = this.toSet().iterator()
