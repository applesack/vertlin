package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Encoder

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午8:32
 */

typealias EventbusConsumer = suspend CoroutineScope.(String) -> String


class EventbusApiBuilder(val callback: EventbusConsumer) {

    val eventbus by inject(EventBus::class)

    lateinit var address: String

    suspend inline operator fun <reified Out> invoke(params: String): Out {
        val content = eventbus.request<String>(address, params).await().body()
        return Encoder.decode(content)
    }

}
