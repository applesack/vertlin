package xyz.scootaloo.vertlin.dav.util

import io.vertx.ext.web.RoutingContext
import xyz.scootaloo.vertlin.dav.constant.Constant

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 下午2:22
 */
object ContextUtils {

    fun displayCurrentUserName(ctx: RoutingContext): String {
        return ctx.user()?.principal()?.getString(Constant.USERNAME) ?: Constant.UNKNOWN
    }

}
