package xyz.scootaloo.vertlin.boot.resolver

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.Ordered

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午1:47
 */
interface ContextServiceManifest {

    fun name(): String

    fun context(): String

    suspend fun register(vertx: Vertx)

    fun getOrder(): Int {
        return Ordered.DEFAULT
    }

}
