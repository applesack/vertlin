package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import xyz.scootaloo.vertlin.boot.Service
import java.util.*
import kotlin.reflect.KClass

/**
 * [EventBus]解码器配置
 *
 * 由于这个接口和[EventbusApi]共用了一个服务解析器, 所以使用这个接口同时必须继承[EventbusApi]才能被系统识别并处理
 *
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午8:27
 */
interface EventbusDecoder : Service {

    /**
     * 根据入参, 配置反序列化器
     */
    fun decoders(builder: EventbusDecoderBuilder)

}

class EventbusDecoderBuilder(
    internal val decoders: MutableList<JsonCodec<*>> = LinkedList()
) {

    /**
     * 创建一个类型解码器, 其中[type]是目标类型, 你需要在[convert]中, 将一个[JsonObject]对象转换为目标类型;
     *
     * **注意:**
     * - **在[Vertx]中, 整个[EventBus]对于每一个类型都只能有一个默认解码器;**
     * - **当使用此方法创建了多个同类型的解码器时, 只有其中一个会生效(随机), 其余的会被忽略**
     * - **为了消除不必要的bug, 请为每个类型只创建一个解码器**
     */
    fun <T : Any> codec(
        type: KClass<T>, convert: (JsonObject) -> T
    ) {
        this.decoders.add(JsonCodec(type, convert))
    }

}
