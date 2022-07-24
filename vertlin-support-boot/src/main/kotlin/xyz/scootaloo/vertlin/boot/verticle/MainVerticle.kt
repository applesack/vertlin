package xyz.scootaloo.vertlin.boot.verticle

import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午9:29
 */
class MainVerticle(
    private val systemManifest: List<ContextServiceManifest>,
    private val contexts: Map<String, List<ContextServiceManifest>>
) : BaseVerticle(), Helper {

    private val log = getLogger()
    private val deploymentIds = ArrayList<String>()
    private val contextName = Constant.SYSTEM

    override suspend fun start() {
        registerResources(contextName)

        val result = runCatching {
            registerServices(contextName, systemManifest)
            deployTemplateVerticle()
        }

        if (result.isSuccess) {
            makeServerAsFinished()
            publishInitEvent()
        } else {
            result.getOrThrow()
        }
    }

    override suspend fun stop() {
        if (deploymentIds.isEmpty()) {
            return
        }
        for (deploymentId in deploymentIds) {
            try {
                vertx.undeploy(deploymentId).await()
            } catch (error: Throwable) {
                log.error(error)
            }
        }
        deploymentIds.clear()
    }

    private suspend fun deployTemplateVerticle() {
        val verticle = ArrayList<TemplateVerticle>(contexts.size)
        for ((name, manifests) in contexts) {
            verticle.add(TemplateVerticle(name, manifests))
        }

        try {
            for (v in verticle) {
                val deploymentId = vertx.deployVerticle(v).await()
                deploymentIds.add(deploymentId)
            }
        } catch (error: Throwable) {
            stop()
            shutdown()
        }
    }

    private fun makeServerAsFinished() {

    }

    private fun publishInitEvent() {
        vertx.eventBus().publish(serviceLifeCycleAddress, "")
    }

    private fun shutdown() {
        vertx.close()
    }

}
