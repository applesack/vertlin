package xyz.scootaloo.vertlin.web.resolver

import io.vertx.core.http.ServerWebSocket
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ResourcesPublisher
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.resolver.UnreachableManifest
import xyz.scootaloo.vertlin.web.WebSocketRegister
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 上午10:23
 */
class WebSocketResolver : ServiceResolver(WebSocketRegister::class) {

    override fun solve(type: KClass<*>, service: Service?, publisher: ResourcesPublisher) {
        val instance = (service ?: return) as WebSocketRegister
        val manifest = WebSocketManifest(instance::handle)
        publisher.registerManifest(manifest)
    }

    class WebSocketManifest(val wsHandler: suspend (ServerWebSocket) -> Unit) :
        ContextServiceManifest by UnreachableManifest

}
