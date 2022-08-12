package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午11:49
 */
object OptionsService {

    private const val public =
        "OPTIONS, PROPFIND, GET, HEAD, PUT, DELETE, COPY, MOVE, MKCOL, LOCK, UNLOCK, POST"

    // DAV: 1,2
    // MS-Author-Via: DAV
    // Accept-Ranges: bytes
    // Allow:

    private val headers = mapOf(
        "DAV" to "1,2",
        "Allow" to public,
        "MS-Author-Via" to "DAV",
        "Accept-Ranges" to "bytes"
    )

    fun handle(ctx: RoutingContext) {
        val response = ctx.response()
        for ((key, value) in headers) {
            response.putHeader(key, value)
        }
        response.end()
    }

}
