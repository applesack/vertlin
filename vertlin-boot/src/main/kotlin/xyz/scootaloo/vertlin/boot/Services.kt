package xyz.scootaloo.vertlin.boot

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午10:51
 */
interface Component


interface Service : Component


interface InjectableService : Service


interface Closeable : Component {

    suspend fun close() {}

}


interface ServiceLifeCycle : Closeable {

    suspend fun initialize() {}

}


interface ContextOnly


interface LazyInit
