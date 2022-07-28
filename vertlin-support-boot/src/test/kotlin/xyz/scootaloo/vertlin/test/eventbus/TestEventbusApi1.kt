package xyz.scootaloo.vertlin.test.eventbus

import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.BootManifest
import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApi
import xyz.scootaloo.vertlin.boot.internal.inject
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午10:03
 */
class TestEventbusApi1 : BootManifest {

    private val log = X.getLogger(this::class)
    private val testApi by inject(TestApi::class)

    @Test
    fun test(): Unit = runBlocking {
        runApplication(arrayOf()).await()
        log.info("before call")
        log.info("after call: " + testApi.hello())
    }

    override fun services(): Collection<KClass<out Service>> {
        return listOf(TestApi::class)
    }

}

@Context("test")
class TestApi : EventbusApi() {

    private val log = X.getLogger(this::class)

    val hello = api {
        log.info("invoking")
        "hello world"
    }

}
