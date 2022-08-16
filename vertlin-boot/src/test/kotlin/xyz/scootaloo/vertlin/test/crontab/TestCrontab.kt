package xyz.scootaloo.vertlin.test.crontab

import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.BootManifest
import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.crontab.CrontabAdapter
import xyz.scootaloo.vertlin.boot.crontab.CrontabManager
import xyz.scootaloo.vertlin.boot.internal.inject
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/25 下午3:43
 */
class TestCrontab : BootManifest() {

    @Test
    fun test(): Unit = runBlocking {
        runApplication(arrayOf()).await()
        delay(100000)
    }

}


@Context("test")
class TestCrontabService1 : CrontabAdapter() {

    private val manager by inject(CrontabManager::class)

    private val log = X.getLogger(this::class)

    override val id = "hello"

    override var delay = 500L

    override fun run(currentTimeMillis: Long) {
        log.info(manager)
        log.info("hello service1")
    }

}

class TestCrontabService2 : CrontabAdapter() {

    private val log = X.getLogger(this::class)

    override val id = "hello"

    override var delay = 1000L

    override fun run(currentTimeMillis: Long) {
        log.info("hello service2")
    }

}
