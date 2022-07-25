package xyz.scootaloo.vertlin.boot.crontab

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午10:51
 */
class CrontabManifest(
    internal val context: String,
    internal val crontab: Crontab
) : ContextServiceManifest {

    override fun name(): String {
        throw UnsupportedOperationException()
    }

    override fun context(): String {
        return context
    }

    override suspend fun register(vertx: Vertx) {
        throw UnsupportedOperationException()
    }

}
