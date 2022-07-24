package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import xyz.scootaloo.vertlin.boot.internal.Container

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午8:32
 */

typealias EventbusConsumer<T> = suspend CoroutineScope.(JsonObject) -> T


class EventbusApiBuilder<T : Any>(
    val codec: JsonCodec<T>? = null,
    val callback: EventbusConsumer<T>
) : EventbusHandle<T> {

    private val eventbus by lazy { Container.getVertx().eventBus() }

    internal lateinit var address: String

    override suspend fun invoke(json: JsonObject): T {
        return request(json).body()
    }

    override suspend fun request(json: JsonObject): Message<T> {
        return eventbus.request<T>(address, json).await()
    }

}
