package xyz.scootaloo.vertlin.boot

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午10:51
 */
interface Service


interface Resource : Service


interface InjectableService : Service


interface EventbusApi : InjectableService {

    companion object {
        suspend operator fun <T> invoke(block: suspend CoroutineScope.() -> T): T {
            return coroutineScope {
                block()
            }
        }

        @JvmName("invokeCoroutineScopeT")
        suspend operator fun <T> (suspend CoroutineScope.() -> T).unaryPlus() {
            return coroutineScope {
                this@unaryPlus()
            }
        }

    }

}


interface Closeable {

    suspend fun close() {}

}


interface ServiceLifeCycle : Closeable {

    suspend fun initialize() {}

}
