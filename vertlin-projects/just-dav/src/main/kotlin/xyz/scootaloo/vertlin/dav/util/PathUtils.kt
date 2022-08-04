package xyz.scootaloo.vertlin.dav.util

import io.vertx.core.net.impl.URIDecoder
import java.net.URLEncoder

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午3:16
 */
object PathUtils {

    fun decodeUriComponent(uri: String): String {
        return URIDecoder.decodeURIComponent(uri)
    }

    fun encodeUriComponent(uri: String): String {
        return URLEncoder.encode(uri, "UTF-8").replace("%2F", "/")
    }

}
