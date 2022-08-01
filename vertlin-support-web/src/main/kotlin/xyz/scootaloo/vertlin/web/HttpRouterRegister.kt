package xyz.scootaloo.vertlin.web

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.internal.CoroutineResource
import xyz.scootaloo.vertlin.boot.internal.inject

/**
 * @author flutterdash@qq.com
 * @since 2022/7/29 上午11:11
 */
abstract class HttpRouterRegister(val mountPoint: String) : Service {

    private val coroutine by inject(CoroutineResource::class)

    abstract fun register(router: Router)

    protected fun Router.get(path: String, handle: CoroutineReqHandler) = get(path).process(handle)

    protected fun Router.post(path: String, handle: CoroutineReqHandler) = post(path).process(handle)

    protected fun Router.head(path: String, handle: CoroutineReqHandler) = head(path).process(handle)

    protected fun Router.options(path: String, handle: CoroutineReqHandler) = options(path).process(handle)

    protected fun Router.put(path: String, handle: CoroutineReqHandler) = put(path).process(handle)

    protected fun Router.delete(path: String, handle: CoroutineReqHandler) = delete(path).process(handle)

    protected fun Router.trace(path: String, handle: CoroutineReqHandler) = trace(path).process(handle)

    protected fun Router.connect(path: String, handle: CoroutineReqHandler) = connect(path).process(handle)

    protected fun Router.patch(path: String, handle: CoroutineReqHandler) = patch(path).process(handle)

    protected fun Router.method(m: HttpMethod, path: String, handle: CoroutineReqHandler) =
        route(m, path).process(handle)

    private fun Route.process(handle: CoroutineReqHandler) {
        handler { ctx ->
            coroutine.launch {
                handle(ctx)
            }
        }
    }

}
