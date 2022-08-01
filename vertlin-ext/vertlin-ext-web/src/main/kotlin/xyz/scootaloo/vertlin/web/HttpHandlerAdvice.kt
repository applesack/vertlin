package xyz.scootaloo.vertlin.web

import io.vertx.ext.web.RoutingContext
import xyz.scootaloo.vertlin.boot.Service

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 下午10:32
 */
interface HttpHandlerAdvice : Service {

    fun errorHandler(error: Throwable, ctx: RoutingContext)

}
