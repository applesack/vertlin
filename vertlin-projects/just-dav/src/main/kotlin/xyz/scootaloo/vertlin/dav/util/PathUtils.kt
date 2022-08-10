package xyz.scootaloo.vertlin.dav.util

import io.vertx.core.net.impl.URIDecoder
import java.io.File
import java.net.URLEncoder
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午3:16
 */
object PathUtils {

    /**
     * '+' -> ' '
     * '%2B' -> '+'
     */
    fun decodeUriComponent(uri: String, plus: Boolean = true): String {
        return URIDecoder.decodeURIComponent(uri, plus)
    }

    /**
     * ' ' -> '+'
     * '+' -> '%2B'
     */
    fun encodeUriComponent(uri: String): String {
        return URLEncoder.encode(uri, "UTF-8")
            .replace("%2F", "/")
    }

    private val separator = File.separatorChar

    fun filename(path: String): String {
        val idx = path.lastIndexOf(separator)
        if (idx < 0) return ""
        return path.substring(idx + 1)
    }

    fun buildPath(baseAbsolutePath: String, vararg addition: String): String {
        return Path.of(baseAbsolutePath, *addition).absolutePathString()
    }

}
