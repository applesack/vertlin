package xyz.scootaloo.vertlin.boot.util

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 下午11:36
 */
object CUtils {

    fun <K, V, C : MutableCollection<V>> grouping(
        map: MutableMap<K, C>, key: K, value: V, init: () -> C
    ) {
        val collection = map[key] ?: init()
        collection.add(value)
        map[key] = collection
    }

}
