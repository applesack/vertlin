package xyz.scootaloo.vertlin.boot.resolver

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 下午9:07
 */
interface ServiceReducer<T : ContextServiceManifest> {

    fun acceptSourceType(): KClass<T>

    fun reduce(services: MutableList<ContextServiceManifest>): ContextServiceManifest

    fun transfer(services: MutableList<ContextServiceManifest>): Collection<T> {
        @Suppress("UNCHECKED_CAST")
        return services.map { it as T }
    }

}
