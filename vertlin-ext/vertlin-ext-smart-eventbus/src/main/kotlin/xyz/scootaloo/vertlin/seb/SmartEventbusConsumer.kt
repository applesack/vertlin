package xyz.scootaloo.vertlin.seb

import io.vertx.core.eventbus.EventBus
import io.vertx.kotlin.coroutines.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.scootaloo.vertlin.boot.internal.inject

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 上午10:47
 */

abstract class SmartEventbusConsumerRegister {

    val eventbus by inject(EventBus::class)

    lateinit var address: String

    internal fun registerConsumer() {
    }

}

class Test(val name: String)

class SmartEventbusConsumer1<T, R>(val callback: (T) -> R) : SmartEventbusConsumerRegister() {

    suspend inline operator fun <reified T, reified R> invoke(p: T): R {
        val serialized = Json.encodeToString(p)
        val deserialized = eventbus.request<String>(address, serialized).await().body()
        return Json.decodeFromString(deserialized)
    }

}
