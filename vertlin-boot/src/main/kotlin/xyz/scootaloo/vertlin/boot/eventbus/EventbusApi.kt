package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xyz.scootaloo.vertlin.boot.InjectableService

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午11:28
 */
abstract class EventbusApi : InjectableService {

    /**
     * 创建一个[EventBus]的消费端
     *
     * 需要注意的一点就是[consumer]这个回调的返回值, 从回调中返回值然后传递到接收者, 会经过序列化和反序列化的处理;
     * 序列化操作默认会将返回值转化为JsonObject(基础类型除外), 然后由对应的泛序列化器将JsonObject转化成目标类型;
     *
     * 系统默认只包含基础类型的反序列化器:
     * `null`, `boolean`, `byte`, `short`, `int`, `float`, `double`,
     * `long`, `String`, `JsonObject`, `JsonArray`,
     * 如果你的回调的返回值不属于以上11种之一, 则需要考虑实现[EventbusDecoder]接口, 并注册对应类型的反序列化器
     */
    protected fun <T : Any> api(
        consumer: EventbusConsumer<T>
    ): EventbusApiBuilder<T> {
        return EventbusApiBuilder(consumer)
    }

    protected inline fun <reified T> JsonObject.asPojo(): T {
        return Json.decodeFromString(this.toString())
    }

}
