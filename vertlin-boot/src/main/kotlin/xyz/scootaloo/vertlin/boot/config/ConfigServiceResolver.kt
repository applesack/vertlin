package xyz.scootaloo.vertlin.boot.config

import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.resolver.ResourcesPublisher
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午3:41
 */
object ConfigServiceResolver : ServiceResolver(Config::class) {

    override fun solve(type: KClass<*>, service: Service?, publisher: ResourcesPublisher) {
        val result = createInstance(type)
        if (result.isFailure) {
            throw TypeMismatchingException(type, result.exceptionOrNull())
        }

        val instance = result.getOrThrow()
        publisher.publishSharedSingleton(instance)
    }

    private fun createInstance(type: KClass<*>): Result<Any> {
        return kotlin.runCatching {
            val prefixAnno = type.findAnnotations(Prefix::class)
            val prefix = if (prefixAnno.isNotEmpty()) {
                prefixAnno.first().value
            } else {
                ""
            }

            val primaryConstructor = type.primaryConstructor!!
            val params = primaryConstructor.parameters
            val args = HashMap<KParameter, Any?>()
            for (idx in params.indices) {
                val param = params[idx]
                val configName = if (prefix.isNotEmpty())
                    "${prefix}.${param.name}" else param.name!!
                args[param] = PropertySources.getProperty(configName)
            }

            primaryConstructor.isAccessible = true
            primaryConstructor.callBy(args)
        }
    }

    private class TypeMismatchingException(type: KClass<*>, cause: Throwable?) : RuntimeException(
        "配置异常: 生成目标类型'$type'时遇到异常, 请检查配置文件内容是否正确", cause
    )

}
