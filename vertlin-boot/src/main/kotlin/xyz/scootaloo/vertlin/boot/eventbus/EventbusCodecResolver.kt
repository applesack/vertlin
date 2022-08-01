package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.resolver.*
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 上午10:54
 */
object EventbusCodecResolver : ServiceResolver(EventbusDecoder::class), ManifestReducer {

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type) as EventbusDecoder
        val builder = EventbusDecoderBuilder()
        instance.decoders(builder)
        for (codec in builder.decoders) {
            // 当有多个属性都引用了同一个类型的解码器, 则会导致重复注册
            // 当出现重复注册问题时, 只有最早注册的解码器会生效, 而后续注册的解码器会当作异常抛出
            // 所以本解析器实现了ServiceReducer接口, 可以将多余的类型解码器约简, 消除重复注册的异常
            manager.registerManifest(codec)
        }
    }

    override fun reduce(manager: ManifestManager) {
        val services = manager.extractManifests(JsonCodec::class).map {
            @Suppress("UNCHECKED_CAST")
            it as JsonCodec<Any>
        }
        manager.registerManifest(EventbusCodecManifest(services))
    }


    private class EventbusCodecManifest(
        private val codecs: Collection<JsonCodec<Any>>
    ) : ContextServiceManifest {

        override fun name(): String {
            return "eventbus-codec"
        }

        override fun context(): String {
            return Constant.SYSTEM
        }

        override suspend fun register(vertx: Vertx) {
            val noDuplicateCodecs = codecs.associateBy {
                TypeUtils.solveQualifiedName(it.type)
            }.values
            val eventbus = vertx.eventBus()
            for (codec in noDuplicateCodecs) {
                val msgCodec = codec.toMessageCodec()
                eventbus.registerDefaultCodec(codec.type.java, msgCodec)
            }
        }

    }

}
