package xyz.scootaloo.vertlin.test.util

import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import xyz.scootaloo.vertlin.boot.util.LRUCache

/**
 * @author flutterdash@qq.com
 * @since 2022/8/16 下午10:14
 */
class LRUTest : TestDSL {

    @Test
    fun test() {
        val cache = LRUCache<String, String>(3)
        cache["1"] = "1"
        cache["2"] = "2"
        cache["3"] = "3"
        cache.log()
        cache["4"] = "4"
        cache.log()
    }

    @Test
    fun test2() {
        val cache = LRUCache<String, String>(100)
        cache["1"] = "1"
        cache["2"] = "2"
        cache["3"] = "3"

        cache.remove("3")

        cache["3"].log()
    }

}
