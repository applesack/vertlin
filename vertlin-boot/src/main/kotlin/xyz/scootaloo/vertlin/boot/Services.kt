package xyz.scootaloo.vertlin.boot

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午10:51
 */
interface Component


interface Service : Component


interface InjectableService : Service


interface Closeable {

    suspend fun close() {}

}


interface ServiceLifeCycle : Closeable {

    suspend fun initialize() {}

}


interface ContextOnly


interface LazyInit


class Holder<T>(
    val type: KClass<*>,
    private val create: () -> T
) {
    operator fun invoke(): T {
        return create()
    }
}
