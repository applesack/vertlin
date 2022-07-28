package xyz.scootaloo.vertlin.boot.verticle

import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午9:29
 */
class MainVerticle(
    private val systemManifest: List<ContextServiceManifest>,
    private val contexts: Map<String, List<ContextServiceManifest>>
) : BaseVerticle() {

    private val log = X.getLogger(this::class)
    private val deploymentIds = ArrayList<String>()
    private val contextName = Constant.SYSTEM

    override suspend fun start() {
        registerResources(contextName)

        val result = runCatching {
            registerServices(contextName, systemManifest)
            deployTemplateVerticle()
            listeningLifeCycleEvents(log, systemManifest)
        }

        if (result.isSuccess) {
            makeServerAsFinished()
            publishSystemStartEvent()
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
            val waitList = LinkedList<Deferred<String>>()
            for (v in verticle) {
                waitList.add(async { vertx.deployVerticle(v).await() })
            }
            waitList.joinAll()
            deploymentIds.addAll(waitList.map { it.await() })
        } catch (error: Throwable) {
            stop()
            shutdown()
        }
    }

    private fun makeServerAsFinished() {
        Container.finish()
    }

    private fun shutdown() {
        vertx.close()
    }

}
