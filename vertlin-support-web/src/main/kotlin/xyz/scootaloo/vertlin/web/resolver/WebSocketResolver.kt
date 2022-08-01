package xyz.scootaloo.vertlin.web.resolver

import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ResourcesPublisher
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.resolver.UnreachableManifest
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import xyz.scootaloo.vertlin.web.WebSocketRegister
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 上午10:23
 */
class WebSocketResolver : ServiceResolver(WebSocketRegister::class) {

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type) as WebSocketRegister
        val manifest = WebSocketManifest(instance::handle)
        manager.registerManifest(manifest)
    }

    class WebSocketManifest(val wsHandler: suspend (ServerWebSocket) -> Unit) :
        ContextServiceManifest by UnreachableManifest

}
