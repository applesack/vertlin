package xyz.scootaloo.vertlin.web.resolver

import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.boot.resolver.*
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import xyz.scootaloo.vertlin.web.HttpRouterRegister
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/30 下午2:35
 */
class HttpRouterResolver : ServiceResolver(HttpRouterRegister::class) {

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type) as HttpRouterRegister
        val component = RouterComponent(solveOrder(type), instance.mountPoint, instance::register)
        manager.registerManifest(HttpRouterManifest(component))
    }

    class HttpRouterManifest(
        val component: RouterComponent
    ) : ContextServiceManifest by UnreachableManifest

    class RouterComponent(
        val order: Int,
        val mountPoint: String,
        val config: (Router) -> Unit
    )

}
