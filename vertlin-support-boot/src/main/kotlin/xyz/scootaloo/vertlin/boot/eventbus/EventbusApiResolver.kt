package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.core.ifNotNull
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
            if (!property.javaField!!.type.kotlin.isSubclassOf(EventbusApiBuilder::class)) {
                continue
            }

            property.isAccessible = true
            val builder = property.call(instance) as EventbusApiBuilder<*>
            val qualifiedAddress = qualifiedAddressByProperty(addressPrefix, property)

            builder.address = qualifiedAddress
            manifest.consumers.add(builder)

            builder.codec.ifNotNull {
                manager.registerManifest(it)
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
            val eventbus = vertx.eventBus()
            for (codec in codecs) {
                val msgCodec = codec.toMessageCodec()
                eventbus.registerDefaultCodec(codec.type.java, msgCodec)
            }
        }

    }

}
