package xyz.scootaloo.vertlin.test.eventbus

import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.BootManifest
import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.EventbusApi
import xyz.scootaloo.vertlin.boot.EventbusApi.Companion.invoke
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.internal.CoroutineResource
import xyz.scootaloo.vertlin.boot.internal.inject
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午4:38
 */
@Context("test_eb")
open class TestEventbusApi : EventbusApi {

    open suspend fun test1(): Int {
        delay(1500)
        return 2
    }

}

object TestEventbusBoot : BootManifest {

    val testApi by inject(TestEventbusApi::class)

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        val time = measureTimeMillis {
            runApplication(arrayOf()).await()
        }
        println(time)
        delay(1000)
        println(testApi.test1())
        println(Thread.currentThread().name)
    }

    override fun services(): Collection<KClass<out Service>> {
        return listOf(TestEventbusApi::class)
    }

}
