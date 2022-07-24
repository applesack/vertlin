package xyz.scootaloo.vertlin.boot.eventbus

import net.sf.cglib.proxy.Enhancer
import xyz.scootaloo.vertlin.boot.EventbusApi
import xyz.scootaloo.vertlin.boot.eventbus.impl.EventbusApiInvokeInterceptor
import xyz.scootaloo.vertlin.boot.eventbus.impl.EventbusConsumerImpl
import xyz.scootaloo.vertlin.boot.exception.AbstractMethodException
import xyz.scootaloo.vertlin.boot.exception.CreateSubclassException
import xyz.scootaloo.vertlin.boot.exception.NotSuspendMethodException
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.Factory
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import xyz.scootaloo.vertlin.boot.util.Json2Kotlin as AsyncCaller

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午8:37
 */
object EventbusApiResolver : ServiceResolver() {

    override fun acceptType(): KClass<*> {
        return EventbusApi::class
    }

    override fun solve(type: KClass<*>): ContextServiceManifest {
        val context = solveContext(type)
        val addressPrefix = eventbusServiceAddressPrefix(type)
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type)
        val enhanced = enhanceTargetType(type, addressPrefix)

        val manifest = EventbusApiManifest(context)
        for (method in instance::class.declaredMemberFunctions) {
            checkMethodFormat(type, method)
            val qualifiedAddress = AsyncCaller.qualifiedAddressByMethod(addressPrefix, method)
            manifest.consumers.add(
                EventbusConsumerImpl(context, instance, qualifiedAddress, method)
            )
        }

        return object : ContextServiceManifest by manifest, Factory {
            override fun instanceType(): KClass<*> {
                return type
            }

            override fun getObject(): Any {
                return enhanced
            }
        }
    }

    private fun enhanceTargetType(target: KClass<*>, addressPrefix: String): Any {
        val enhancer = Enhancer()
        enhancer.setSuperclass(target.java)
        enhancer.setCallback(EventbusApiInvokeInterceptor(addressPrefix))
        return kotlin.runCatching {
            enhancer.create()
        }.getOrElse {
            throw CreateSubclassException(target, it)
        }
    }

    private fun checkMethodFormat(type: KClass<*>, method: KFunction<*>) {
        if (!method.isSuspend) {
            throw NotSuspendMethodException(type, method)
        }
        if (method.isAbstract) {
            throw AbstractMethodException(type, method)
        }
    }

    private fun eventbusServiceAddressPrefix(klass: KClass<*>): String {
        val simpleName = klass.simpleName ?: "unknown${Random.nextInt(300)}"
        return "api:$simpleName"
    }

}
