package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ServiceManager
import xyz.scootaloo.vertlin.boot.resolver.ServiceReducer
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * ## 系统服务 [EventbusApi] 的专用解析器
 *
 * 将继承于[EventbusApi]的类中所有通过[EventbusApi.api]接口创建出来的属性注册成[EventBus]的消费者;
 *
 * 同时将api接口提供的解码器注册到系统中
 *
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午8:37
 */
object EventbusApiResolver : ServiceResolver(), ServiceReducer<JsonCodec<*>> {

    override fun acceptType(): KClass<*> {
        return EventbusApi::class
    }

    override fun solve(type: KClass<*>, manager: ServiceManager) {
        val context = solveContext(type)
        val addressPrefix = eventbusServiceAddressPrefix(type)
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type)

        val manifest = EventbusApiManifest(context)
        for (property in instance::class.declaredMemberProperties) {
            val fieldType = property.javaField!!.type.kotlin

            if (fieldType.isSubclassOf(EventbusApiBuilder::class)) {
                property.isAccessible = true
                val builder = property.call(instance) as EventbusApiBuilder<*>
                val qualifiedAddress = qualifiedAddressByProperty(addressPrefix, property)

                builder.address = qualifiedAddress
                manifest.consumers.add(builder)
                continue
            }

            if (instance is EventbusDecoder) {
                val builder = EventbusDecoderBuilder()
                instance.decoders(builder)
                for (codec in builder.decoders) {
                    // 当有多个属性都引用了同一个类型的解码器, 则会导致重复注册
                    // 当出现重复注册问题时, 只有最早注册的解码器会生效, 而后续注册的解码器会当作异常抛出
                    // 所以本解析器实现了ServiceReducer接口, 可以将多余的类型解码器约简, 消除重复注册的异常
                    manager.registerManifest(codec)
                }
            }
        }

        manager.publishSingleton(instance)
        manager.registerManifest(manifest)
    }

    override fun acceptSourceType(): KClass<JsonCodec<*>> {
        return JsonCodec::class
    }

    override fun reduce(services: MutableList<ContextServiceManifest>): ContextServiceManifest {
        val codecs = services.map {
            @Suppress("UNCHECKED_CAST")
            it as JsonCodec<Any>
        }
        return EventbusCodecManifest(codecs)
    }

    private fun qualifiedAddressByProperty(prefix: String, prop: KProperty1<*, *>): String {
        return "$prefix:${prop.name}"
    }

    private fun eventbusServiceAddressPrefix(klass: KClass<*>): String {
        val simpleName = klass.simpleName ?: "unknown${Random.nextInt(300)}"
        return "api:$simpleName"
    }

    private class EventbusCodecManifest(
        private val codecs: List<JsonCodec<Any>>
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
