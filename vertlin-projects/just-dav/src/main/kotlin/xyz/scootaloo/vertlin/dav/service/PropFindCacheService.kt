package xyz.scootaloo.vertlin.dav.service

import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.LRUCache
import xyz.scootaloo.vertlin.dav.application.WebDAV
import java.util.LinkedList

/**
 * @author flutterdash@qq.com
 * @since 2022/8/16 下午11:12
 */
object PropFindCacheService {

    private val webdav by inject(WebDAV::class)
    private val cacheSize by lazy { webdav.cache * 1024 * 1024L }
    private val cache = LRUCache<String, String>(Int.MAX_VALUE)
    private val cacheEnable by lazy { cacheSize != 0L }
    private var used = 0L

    fun getCache(path: String): String? {
        if (!cacheEnable)
            return null
        return cache[path]
    }

    fun putCache(path: String, content: String) {
        if (!cacheEnable) {
            return
        }
        val space = space(content)
        if (used + space >= cacheSize) {
            while (cache.isNotEmpty() && used + space >= cacheSize) {
                val eldest = cache.eldestKey()
                invalidate(eldest)
            }
        }

        used += space
        val previous = cache.put(path, content)
        if (previous != null) {
            used -= space(previous)
        }
    }

    fun invalidate(path: String, recursive: Boolean = false) {
        if (!cacheEnable) {
            return
        }

        val removed = cache.remove(path)
        if (removed != null) {
            used -= space(removed)
        }
        if (recursive) {
            val invalid = LinkedList<String>()
            val pathPrefix = "$path/"
            for (key in cache.keys) {
                if (key.startsWith(pathPrefix)) {
                    invalid.add(key)
                }
            }
            if (invalid.isNotEmpty()) {
                invalid.forEach {
                    invalidate(it)
                }
            }
        }
    }

    private fun space(content: String): Long {
        return content.length * 2L
    }

}
