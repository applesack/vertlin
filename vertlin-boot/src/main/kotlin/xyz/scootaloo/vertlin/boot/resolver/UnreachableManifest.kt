package xyz.scootaloo.vertlin.boot.resolver

import io.vertx.core.Vertx

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 下午4:15
 */
object UnreachableManifest : ContextServiceManifest {

    override fun name(): String {
        throw UnsupportedOperationException()
    }

    override fun context(): String {
        throw UnsupportedOperationException()
    }

    override suspend fun register(vertx: Vertx) {
        throw UnsupportedOperationException()
    }

}
