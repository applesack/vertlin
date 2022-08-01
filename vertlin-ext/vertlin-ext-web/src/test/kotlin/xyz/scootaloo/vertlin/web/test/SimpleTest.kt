package xyz.scootaloo.vertlin.web.test

import io.vertx.ext.web.Router
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.BootManifest
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.web.HttpRouterRegister
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 下午11:49
 */
class SimpleTest : BootManifest {

    @Test
    fun test(): Unit = runBlocking {
        runApplication(arrayOf()).await()
        delay(10000)
    }

    override fun services(): Collection<KClass<out Service>> {
        return listOf(TestRouter::class)
    }

}


class TestRouter : HttpRouterRegister("/*") {

    override fun register(router: Router) {
        router.get("/*") {
            it.end("hello world")
        }
    }

}
