package xyz.scootaloo.vertlin.test.config

import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.BootManifest
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.config.Config
import xyz.scootaloo.vertlin.boot.config.Prefix
import xyz.scootaloo.vertlin.boot.internal.inject
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/28 下午10:14
 */
class TestConfig : BootManifest() {

    private val hello by inject(Hello::class)

    @Test
    fun test(): Unit = runBlocking {
        runApplication(arrayOf()).await()
        println(hello)
    }

}

@Prefix("hello")
data class Hello(
    val name: String,
    val age: Int
) : Config
