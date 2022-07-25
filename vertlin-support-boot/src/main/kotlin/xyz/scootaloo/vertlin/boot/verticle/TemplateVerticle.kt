package xyz.scootaloo.vertlin.boot.verticle

import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午1:19
 */
class TemplateVerticle(
    private val contextName: String,
    private val manifests: List<ContextServiceManifest>
) : BaseVerticle(), Helper {

    private val log = getLogger("$contextName-verticle")

    override suspend fun start() {
        registerResources(contextName)
        registerServices(contextName, manifests)
        listeningLifeCycleEvents(log, manifests)
    }

    override suspend fun stop() {
        closeServices(manifests)
    }

}
