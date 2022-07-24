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
import xyz.scootaloo.vertlin.boot.eventbus.EventbusDecoder
import xyz.scootaloo.vertlin.boot.eventbus.EventbusDecoderBuilder
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
        testCodec.hello2().log()
        testCodec.hello3().log()
    }

    override fun services(): Collection<KClass<out Service>> {
        return listOf(TestCodec::class)
    }

}

@Context("test")
class TestCodec : EventbusApi(), EventbusDecoder, Helper {

    val hello = api {
        Pair("abc", "456")
    }

    val hello2 = api {
        Pair("cdf", "123")
    }

    val hello3 = api {
        Pair("xyz", "789")
    }

    override fun decoders(builder: EventbusDecoderBuilder) {
        builder.codec(Pair::class) {
            val first = it.getString("first")
            val second = it.getString("second")
            Pair(first, second)
        }
    }

}

