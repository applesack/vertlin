package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.json.JsonObject
import xyz.scootaloo.vertlin.boot.InjectableService
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午11:28
 */
abstract class EventbusApi : InjectableService {

    protected fun <T : Any> api(
        consumer: EventbusConsumer<T>
    ): EventbusApiBuilder<T> {
        return EventbusApiBuilder(null, consumer)
    }

    protected fun <T : Any> api(
        codec: JsonCodec<T>, consumer: EventbusConsumer<T>
    ): EventbusApiBuilder<T> {
        return EventbusApiBuilder(codec, consumer)
    }

    protected fun <T : Any> codec(
        type: KClass<T>, convert: (JsonObject) -> T
    ): JsonCodec<T> {
        return JsonCodec(type, convert)
    }

}
