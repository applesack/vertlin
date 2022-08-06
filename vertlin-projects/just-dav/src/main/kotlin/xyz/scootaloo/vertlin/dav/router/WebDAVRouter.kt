package xyz.scootaloo.vertlin.dav.router

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.dav.service.LockService
import xyz.scootaloo.vertlin.dav.service.OptionsService
import xyz.scootaloo.vertlin.dav.service.PropFindService
import xyz.scootaloo.vertlin.web.HttpRouterRegister

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午11:35
 */
class WebDAVRouter : HttpRouterRegister("/*") {

    override fun register(router: Router) = router.run {

        method(HttpMethod.PROPFIND) {
            PropFindService.propFind(it)
        }

        method(HttpMethod.PROPPATCH) {

        }

        method(HttpMethod.MKCOL) {

        }

        get {
            it.end("hello world: ${it.pathParam("*")}")
        }

        head {

        }

        method(HttpMethod.LOCK) {
            LockService.lock(it)
        }

        method(HttpMethod.UNLOCK) {

        }

        options {
            OptionsService.handle(it)
        }

        put {

        }

    }

}
