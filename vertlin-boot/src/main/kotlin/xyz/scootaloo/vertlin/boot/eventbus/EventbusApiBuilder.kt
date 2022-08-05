package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Encoder

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午8:32
 */

typealias EventbusConsumer<T> = suspend CoroutineScope.(String) -> T


class EventbusApiBuilder<Out : Any>(val callback: EventbusConsumer<Out>) {

    val eventbus by inject(EventBus::class)

    lateinit var address: String

    suspend inline operator fun <reified Out> invoke(params: Any): Out {
        val encodedParams = Encoder.simpleEncode2Json(params)
        val content = eventbus.request<String>(address, encodedParams).await().body()
        return Json.decodeFromString(content)
    }

}
