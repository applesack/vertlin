package xyz.scootaloo.vertlin.hello

import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.boot.Order
import xyz.scootaloo.vertlin.boot.Ordered
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.web.HttpRouterRegister

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午9:43
 */
@Order(Ordered.HIGHEST)
class HelloHttpRouter : HttpRouterRegister("/*") {

    private val config by inject(MyConfig::class)

    override fun register(router: Router) = router.run {
        get("/config") {
            it.end(config.name)
        }

        get("/name/:name") {
            val name = it.pathParam("name")
            it.end("hello $name")
        }

        post {
            // 在此编写controller代码
        }

        delete {
            // 在此编写controller代码
        }
    }

}
