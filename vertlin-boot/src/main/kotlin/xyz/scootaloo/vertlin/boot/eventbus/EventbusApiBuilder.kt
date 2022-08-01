package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import xyz.scootaloo.vertlin.boot.internal.inject

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午8:32
 */

typealias EventbusConsumer<T> = suspend CoroutineScope.(JsonObject) -> T


class EventbusApiBuilder<T : Any>(val callback: EventbusConsumer<T>) : EventbusHandle<T> {

    private val eventbus by inject(EventBus::class)

    internal lateinit var address: String

    override suspend fun invoke(params: JsonObject): T {
        return request(params).body()
    }

    override suspend fun request(params: JsonObject): Message<T> {
        return eventbus.request<T>(address, params).await()
    }

}


