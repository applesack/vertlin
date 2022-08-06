package xyz.scootaloo.vertlin.web

import io.vertx.ext.web.RoutingContext

/**
 * @author flutterdash@qq.com
 * @since 2022/8/6 下午5:03
 */

fun RoutingContext.endWithXml(xml: String, charset: String? = null) {
    val respHeaders = response().headers()
    respHeaders[ContentType.CONTENT_TYPE] = ContentType.xml(charset)
    end(xml)
}
