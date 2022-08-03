package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午11:49
 */
object OptionsService {

    private const val public =
        "OPTIONS,TRACE,GET,HEAD,DELETE,PUT,POST,COPY,MOVE,MKCOL,PROPFIND,PROPPATCH,LOCK,UNLOCK"

    private val headers = mapOf(
        "DAV" to "1,2",
        "Allow" to public,
        "MS-Author-Via" to "DAV",
        "public" to public
    )

    fun handle(ctx: RoutingContext) {
        val response = ctx.response()
        for ((key, value) in headers) {
            response.putHeader(key, value)
        }
        response.end()
    }

}
