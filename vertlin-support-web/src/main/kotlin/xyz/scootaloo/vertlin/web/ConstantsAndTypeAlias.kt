package xyz.scootaloo.vertlin.web

import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 下午4:49
 */

typealias CoroutineReqHandler = suspend CoroutineScope.(RoutingContext) -> Unit


internal object Constant {

    const val defContext = "server"

}
