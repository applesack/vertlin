package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:04
 */
class EventbusApiManifest(
    private val context: String,
    val consumers: MutableList<EventbusConsumer> = LinkedList()
) : ContextServiceManifest {

    override fun name(): String {
        return "eventbus"
    }

    override fun context(): String {
        return context
    }

    override suspend fun register(vertx: Vertx) {
        val eventbus = vertx.eventBus()
        for (consumer in consumers) {
            eventbus.consumer(consumer.address()) {
                consumer.handle(it)
            }
        }
    }

}
