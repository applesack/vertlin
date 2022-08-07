package xyz.scootaloo.vertlin.dav.router

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import xyz.scootaloo.vertlin.dav.service.LockService
import xyz.scootaloo.vertlin.dav.service.OptionsService
import xyz.scootaloo.vertlin.dav.service.PropFindService
import xyz.scootaloo.vertlin.dav.service.StaticService
import xyz.scootaloo.vertlin.web.HttpRouterRegister
import java.net.http.HttpResponse.BodyHandler

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
            StaticService.get(it)
        }

        head {
            StaticService.get(it)
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
