package xyz.scootaloo.vertlin.boot.util

import java.lang.IndexOutOfBoundsException
import kotlin.jvm.Throws
import kotlin.math.abs

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 下午3:19
 */
class NameGroup<T : Nameable> {

    private var group: Array<Nameable> = arrayOf()
    private val size: Int get() = group.size
    private var findIdx = -1

    /**
     * ## 检查当前集合中是否存在[name]键
     *
     * @return 是否存在
     */
    fun has(name: String): Boolean {
        if (group.isEmpty()) {
            return false
        }

        val nameHash = hash(name)
        val pivot = binarySearch(nameHash)
        if (pivot < 0)
            return false

        val limit = size
        var l = pivot
        var r = pivot + 1
        var selected: String
        while (l >= 0 || r < limit) {
            if (l >= 0) {
                selected = group[l].name
                if (hash(selected) != nameHash) {
                    l = -1
                } else {
                    if (
                        selected.length == name.length &&
                        doublePointerMatch(selected, name)
                    ) {
                        findIdx = l
                        return true
                    }
                    l--
                }
            }

            if (r < limit) {
                selected = group[r].name
                if (hash(selected) != nameHash) {
                    r = limit
                } else {
                    if (
                        selected.length == name.length &&
                        doublePointerMatch(selected, name)
                    ) {
                        findIdx = r
                        return true
                    }
                    r++
                }
            }
        }

        return false
    }

    /**
     * ## 获取[has]或者[add]操作的结果
     *
     * @exception IndexOutOfBoundsException 如果此调用之前没有调用[has]或者[add], 则可能因为指针未更新而导致越界
     * @return 返回[has]或者[add]的结果
     */
    @Throws(IndexOutOfBoundsException::class)
    fun get(): T {
        @Suppress("UNCHECKED_CAST")
        return group[findIdx] as T
    }

    /**
     * ## 向集合中插入一个成员
     */
    fun add(member: T) {
        if (size == 0) {
            group = arrayOf(member)
            findIdx = 0
            return
        }
        if (has(member.name)) {
            group[findIdx] = member
            return
        }
        val suitable = suitablePlace(member.name)
        val newGroup = Array(size + 1) { idx ->
            if (idx < suitable) {
                group[idx]
            } else if (idx > suitable) {
                group[idx - 1]
            } else {
                member
            }
        }
        findIdx = suitable
        group = newGroup
    }

    /**
     * ## 从集合中删除一个成员
     */
    fun del(member: String) {
        if (!has(member)) {
            return
        }

        if (size == 1) {
            group = arrayOf()
            return
        }

        val newGroup = Array(size - 1) { idx ->
            if (idx < findIdx) {
                group[idx]
            } else {
                group[idx + 1]
            }
        }
        group = newGroup
    }

    private fun suitablePlace(member: String): Int {
        val h = hash(member)
        for (idx in group.indices) {
            if (hash(group[idx].name) > h)
                return idx
        }
        return size
    }

    private fun doublePointerMatch(a: String, b: String): Boolean {
        var head = 0
        var tail = a.length - 1
        while (head < tail) {
            if (a[head] == b[head]) {
                head++
            } else {
                return false
            }

            if (a[tail] == b[tail]) {
                tail--
            } else {
                return false
            }
        }

        if (head == tail)
            return a[head] == b[head]
        return true
    }

    private fun binarySearch(hash: Int): Int {
        var high = size - 1
        var low = 0

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midVal = hash(group[mid].name)

            if (midVal < hash) {
                low = mid + 1
            } else if (midVal > hash) {
                high = mid - 1
            } else {
                return mid
            }
        }

        return -1
    }

    private fun hash(str: String): Int {
        return abs(str.hashCode())
    }

    override fun toString(): String {
        return "NameGroup(group=${group.contentToString()}, findIdx=$findIdx)"
    }

}
