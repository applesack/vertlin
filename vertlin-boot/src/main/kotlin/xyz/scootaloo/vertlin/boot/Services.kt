package xyz.scootaloo.vertlin.boot

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午10:51
 */
interface Service


interface InjectableService : Service

interface Closeable {

    suspend fun close() {}

}


interface ServiceLifeCycle : Closeable {

    suspend fun initialize() {}

}


interface ContextOnly
