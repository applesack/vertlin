package xyz.scootaloo.vertlin.test.eventbus

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.EventbusApi

/**
 * @author flutterdash@qq.com
 * @since 2022/7/23 下午11:23
 */

@Context("eb_test1")
class TestEventbus2 : EventbusApi {
    suspend fun test1(): Int {
        return 3
    }
}

class TestEventbusApi2 {

    @Test
    fun test(): Unit = runBlocking {

    }

}
