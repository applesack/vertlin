package xyz.scootaloo.vertlin.test.eventbus

import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.BootManifest
import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.core.TestDSL
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApi
import xyz.scootaloo.vertlin.boot.internal.inject
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午5:03
 */
class TestEventbusCodec : BootManifest, TestDSL {

    private val testCodec by inject(TestCodec::class)

    @Test
    fun test(): Unit = runBlocking {
        measureTimeMillis {
            runApplication(arrayOf()).await()
        }.log()
        testCodec.hello().log()
    }

    override fun services(): Collection<KClass<out Service>> {
        return listOf(TestCodec::class)
    }

}

@Context("test")
class TestCodec : EventbusApi(), Helper {

    private val log = getLogger()

    private val pair = codec(Pair::class) {
        val first = it.getString("first")
        val second = it.getString("second")
        Pair(first, second)
    }

    val hello = api(pair) {
        Pair("abc", "456")
    }

}

