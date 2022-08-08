package xyz.scootaloo.vertlin.dav.util

import io.vertx.core.net.impl.URIDecoder
import java.net.URLEncoder

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

}
