package xyz.scootaloo.vertlin.dav.router

import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.dav.service.*
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
            CollectionService.mkdir(it)
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
            UploadService.put(it)
        }

        delete {
            DeleteService.delete(it)
        }

        method(HttpMethod.COPY) {
            CopyService.copy(it)
        }

        method(HttpMethod.MOVE) {
            MoveService.move(it)
        }

    }

}
