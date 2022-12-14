package xyz.scootaloo.vertlin.web.resolver

import io.vertx.core.Vertx
import io.vertx.core.file.FileSystem
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.impl.logging.Logger
import io.vertx.core.net.PfxOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.core.net.jksOptionsOf
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.internal.CoroutineResource
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.resolver.*
import xyz.scootaloo.vertlin.web.Constant
import xyz.scootaloo.vertlin.web.HttpServerConfig
import xyz.scootaloo.vertlin.web.HttpSslConfig
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
    private val ssl by inject(HttpSslConfig::class)

    override fun solve(type: KClass<*>, service: Service?, publisher: ResourcesPublisher) {
        throw UnsupportedOperationException()
    }

    override fun reduce(manager: ManifestManager) {
        val routers = manager.extractManifests(HttpRouterManifest::class).map { it.component }
        val websocketHandlers = manager.extractManifests(WebSocketManifest::class).map { it.wsHandler }
        val finalManifest = HttpServerManifest(log, routers, websocketHandlers, config, ssl)

        log.info("Vert.x HttpServer initialized with port(s): ${config.port} (http)")

        manager.registerManifest(finalManifest)
    }

    class HttpServerManifest(
        private val log: Logger,
        private val httpRouters: List<HttpRouterResolver.RouterComponent>,
        private val websocketHandler: List<suspend (ServerWebSocket) -> Unit>,
        private val config: HttpServerConfig,
        private val sslConfig: HttpSslConfig,
    ) : ContextServiceManifest {

        private val coroutine by inject(CoroutineResource::class)
        private lateinit var server: HttpServer
        private val fs by inject(FileSystem::class)

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

            log.info("Vert.x Web started on port(s) ${config.port} (http)")
        }

        private suspend fun httpServerOptions(): HttpServerOptions {
            val httpServerOptions = httpServerOptionsOf()
            if (sslConfig.enable) {
                // https://www.cnblogs.com/SparkMore/p/14067340.html
                if (!fs.exists(sslConfig.path).await()) {
                    log.info("ssl配置'${sslConfig.path}'未找到, 所以此配置被禁用")
                }

                val buffer = fs.readFile(sslConfig.path).await()
                httpServerOptions.isSsl = true
                httpServerOptions.keyStoreOptions = jksOptionsOf()
                    .setPassword(sslConfig.password)
                    .setValue(buffer)
            }
            return httpServerOptions
        }

        private fun bindReqRouters(vertx: Vertx): Router {
            val rootRouter = Router.router(vertx)
            if (config.enableLog) {
                rootRouter.route().handler {
                    logRequest(it)
                    it.next()
                }
            }

            rootRouter.route().handler(bodyHandler())
            if (httpRouters.isEmpty()) {
                log.warn("http-server: 未找到任何请求处理器实现")
                return rootRouter
            }

            for (router in httpRouters) {
                val subRouter = Router.router(vertx)
                router.config(subRouter)
                rootRouter.route(router.mountPoint).subRouter(subRouter)
            }

            if (config.prefix.isNotBlank()) {
                val prefix = if (config.prefix.last() == '*')
                    config.prefix
                else
                    config.prefix + "*"
                val root = Router.router(vertx)
                root.route(prefix).subRouter(rootRouter)
                return root
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

        private fun bodyHandler(): BodyHandler {
            return BodyHandler.create()
                .setBodyLimit(config.bodyLimit)
        }

        private fun logRequest(ctx: RoutingContext) {
            val request = ctx.request()
            val remote = request.remoteAddress().hostAddress()
            val method = request.method()
            val uri = request.uri()
            RequestRecorder.log.info("$remote $method $uri")
        }

    }

    object RequestRecorder {
        val log = X.getLogger(this::class)
    }


}
