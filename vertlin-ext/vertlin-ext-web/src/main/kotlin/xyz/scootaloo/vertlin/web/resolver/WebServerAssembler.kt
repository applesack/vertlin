package xyz.scootaloo.vertlin.web.resolver

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.Logger
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.internal.CoroutineResource
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.resolver.*
import xyz.scootaloo.vertlin.web.Constant
import xyz.scootaloo.vertlin.web.HttpServerConfig
import xyz.scootaloo.vertlin.web.resolver.HttpRouterResolver.HttpRouterManifest
import xyz.scootaloo.vertlin.web.resolver.WebSocketResolver.WebSocketManifest
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 上午10:16
 */
class WebServerAssembler : ServiceResolver(UnreachableService::class), ManifestReducer {

    private val log = X.getLogger(this::class)
    private val config by inject(HttpServerConfig::class)

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
        throw UnsupportedOperationException()
    }

    override fun reduce(manager: ManifestManager) {
        val routers = manager.extractManifests(HttpRouterManifest::class).map { it.component }
        val websocketHandlers = manager.extractManifests(WebSocketManifest::class).map { it.wsHandler }
        val finalManifest = HttpServerManifest(log, routers, websocketHandlers, config)

        log.info("Vert.x HttpServer initialized with port(s): ${config.port} (http)")

        manager.registerManifest(finalManifest)
    }

    class HttpServerManifest(
        private val log: Logger,
        private val httpRouters: List<HttpRouterResolver.RouterComponent>,
        private val websocketHandler: List<suspend (ServerWebSocket) -> Unit>,
        private val config: HttpServerConfig
    ) : ContextServiceManifest {

        private val coroutine by inject(CoroutineResource::class)
        private lateinit var server: HttpServer

        override fun name(): String {
            return "http-server"
        }

        override fun context(): String {
            return Constant.defContext
        }

        override suspend fun register(vertx: Vertx) {
            log.info("Staring service [Vertx Web]")

            server = vertx.createHttpServer(httpServerOptions())
            server.requestHandler(bindReqRouters(vertx))
            bindWebSocketHandler()
            server.listen(config.port).await()

            log.info("Vert.x Web started on port(s) ${config.port} (HTTP)")
        }

        private fun httpServerOptions(): HttpServerOptions {
            return httpServerOptionsOf()
        }

        private fun bindReqRouters(vertx: Vertx): Router {
            val rootRouter = Router.router(vertx)
            rootRouter.route().handler(BodyHandler.create())
            if (httpRouters.isEmpty()) {
                log.warn("http-server: 未找到任何请求处理器实现")
                return rootRouter
            }

            for (router in httpRouters) {
                val subRouter = Router.router(vertx)
                router.config(subRouter)
                rootRouter.route(router.mountPoint).subRouter(subRouter)
            }

            return rootRouter
        }

        private fun bindWebSocketHandler() {
            if (websocketHandler.isEmpty()) {
                return
            }
            if (websocketHandler.size > 1) {
                log.warn("http-server: websocket处理器不止一个, 但最终只会有一个被装配;")
            }

            val finalHandler = websocketHandler.first()
            server.webSocketHandler { ws ->
                coroutine.launch {
                    finalHandler(ws)
                }
            }
        }

    }

}
