package xyz.scootaloo.vertlin.boot.verticle

import io.vertx.core.impl.logging.Logger
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import xyz.scootaloo.vertlin.boot.Closeable
import xyz.scootaloo.vertlin.boot.ServiceLifeCycle
import xyz.scootaloo.vertlin.boot.exception.DeployServiceException
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.internal.impl.CoroutineResourceImpl
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest

/**
 * @author flutterdash@qq.com
 * @since 2022/7/20 上午10:12
 */
abstract class BaseVerticle : CoroutineVerticle() {

    protected val serviceLifeCycleAddress = "sys:cyl:init"

    protected fun registerResources(contextName: String) {
        Container.registerContextMapper(Thread.currentThread().name, contextName)
        Container.registerCoroutineEntrance(
            contextName, CoroutineResourceImpl(this)
        )
    }

    protected suspend fun registerServices(
        contextName: String, manifests: Collection<ContextServiceManifest>
    ) {
        for (manifest in manifests) {
            try {
                manifest.register(vertx)
            } catch (error: Throwable) {
                throw DeployServiceException(manifest.name(), contextName, error)
            }
        }
    }

    protected fun listeningLifeCycleEvents(log: Logger, manifests: Collection<ContextServiceManifest>) {
        vertx.eventBus().consumer<String>(serviceLifeCycleAddress) {
            launch {
                initializeServices(log, manifests)
            }
        }
    }

    private suspend fun initializeServices(log: Logger, manifests: Collection<ContextServiceManifest>) {
        for (manifest in manifests) {
            if (manifest is ServiceLifeCycle) {
                try {
                    manifest.initialize()
                } catch (error: Throwable) {
                    log.error("初始化异常: 执行服务生命周期时遇到异常; 阶段[初始化]", error)
                }
            }
        }
    }

    protected suspend fun closeServices(manifests: Collection<ContextServiceManifest>) {
        for (manifest in manifests) {
            if (manifest is Closeable) {
                try {
                    manifest.close()
                } catch (ignore: Throwable) {
                }
            }
        }
    }

}
