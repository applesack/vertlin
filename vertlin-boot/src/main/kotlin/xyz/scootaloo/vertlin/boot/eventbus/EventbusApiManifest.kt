package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.internal.CoroutineResource
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.util.Encoder
import java.util.LinkedList

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:04
 */
class EventbusApiManifest(
    private val context: String,
    internal val consumers: MutableList<EventbusApiBuilder<*>> = LinkedList()
) : ContextServiceManifest {

    private val coroutine by inject(CoroutineResource::class)

    override fun name(): String {
        return "eventbus"
    }

    override fun context(): String {
        return context
    }

    override suspend fun register(vertx: Vertx) {
        val eventbus = vertx.eventBus()
        for (consumer in consumers) {
            eventbus.consumer(consumer.address) {
                coroutine.launch end@{
                    val ret = consumer.callback(this, it.body())
                    val encoded = Encoder.simpleEncode2Json(ret)
                    it.reply(encoded)
                }
            }
        }
    }

}
