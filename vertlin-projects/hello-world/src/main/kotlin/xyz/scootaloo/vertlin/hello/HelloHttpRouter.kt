package xyz.scootaloo.vertlin.hello

import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.boot.Order
import xyz.scootaloo.vertlin.boot.Ordered
import xyz.scootaloo.vertlin.web.HttpRouterRegister

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午9:43
 */
@Order(Ordered.HIGHEST)
class HelloHttpRouter : HttpRouterRegister("/*") { // 拦截器

    override fun register(router: Router) = router.run {
        any {
            val auth = it.request().getHeader("")
            it.fail(403)
        }
        router.route().handler {

        }
        get("/*") {
            it.end("hello world")
        }
    }

}
