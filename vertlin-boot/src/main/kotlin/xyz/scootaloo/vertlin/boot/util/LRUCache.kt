package xyz.scootaloo.vertlin.boot.util

/**
 * @author flutterdash@qq.com
 * @since 2022/8/16 下午8:54
 */
class LRUCache<K : Comparable<K>, V : Any>(private val maxSize: Int) : MutableMap<K, V> {

    private val map = HashMap<K, Node<K, V>>()
    private val head = Node<K, V>()
    private val tail = Node<K, V>()

    init {
        clear()
    }

    fun eldestKey(): K {
        return tail.prev.key
    }

    private fun moveToHead(node: Node<K, V>) {
        delete(node)
        addToHead(node)
    }

    private fun addToHead(node: Node<K, V>) {
        node.prev = head
        node.next = head.next

        head.next.prev = node
        head.next = node
    }

    private fun removeTail(): Node<K, V> {
        val target = tail.prev
        return delete(target)
    }

    private fun delete(node: Node<K, V>): Node<K, V> {
        node.prev.next = node.next
        node.next.prev = node.prev
        return node
    }

    private fun createNode(key: K, value: V): Node<K, V> {
        val node = Node<K, V>()
        node.key = key
        node.value = value
        return node
    }

    private class Node<K : Comparable<K>, V : Any> {
        lateinit var key: K
        lateinit var value: V
        lateinit var prev: Node<K, V>
        lateinit var next: Node<K, V>
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return map.map { it.key to it.value.value }.toMap().toMutableMap().entries
        }

    override val keys: MutableSet<K>
        get() {
            return map.keys
        }

    override val size: Int get() = map.size

    override val values: MutableCollection<V>
        get() {
            return map.values.map { it.value }.toMutableList()
        }

    override fun clear() {
        map.clear()
        head.next = tail
        tail.prev = head
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun remove(key: K): V? {
        val previous = map.remove(key)
        if (previous != null) {
            delete(previous)
        }
        return previous?.value
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) {
            put(k, v)
        }
    }

    override fun put(key: K, value: V): V? {
        val previous = map[key]
        if (previous == null) {
            val node = createNode(key, value)
            if (map.size == maxSize) {
                removeTail()
            }
            map[key] = node
            addToHead(node)
        } else {
            previous.value = value
            moveToHead(previous)
        }


        return previous?.value
    }

    override fun get(key: K): V? {
        val previous = map[key]
        if (previous != null) {
            moveToHead(previous)
        }
        return previous?.value
    }

    override fun containsValue(value: V): Boolean {
        return map.values.find { it.value == value } != null
    }

    override fun containsKey(key: K): Boolean {
        return get(key) != null
    }

}
