package xyz.scootaloo.vertlin.web

import io.vertx.core.http.ServerWebSocket
import xyz.scootaloo.vertlin.boot.Service

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 上午10:16
 */
interface WebSocketRegister : Service {

    suspend fun handle(ws: ServerWebSocket)

}
