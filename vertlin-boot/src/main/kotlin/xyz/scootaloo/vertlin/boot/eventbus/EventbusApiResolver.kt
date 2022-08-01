package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.EventBus
import xyz.scootaloo.vertlin.boot.resolver.ResourcesPublisher
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
object EventbusApiResolver : ServiceResolver(EventbusApi::class) {

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
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
        }

        manager.publishSharedSingleton(instance)
        manager.registerManifest(manifest)
    }

    private fun qualifiedAddressByProperty(prefix: String, prop: KProperty1<*, *>): String {
        return "$prefix:${prop.name}"
    }

    private fun eventbusServiceAddressPrefix(klass: KClass<*>): String {
        val simpleName = klass.simpleName ?: "unknown${Random.nextInt(300)}"
        return "api:$simpleName"
    }

}
