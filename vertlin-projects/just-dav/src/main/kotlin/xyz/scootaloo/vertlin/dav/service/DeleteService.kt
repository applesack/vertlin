package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext
import xyz.scootaloo.vertlin.dav.domain.AccessBlock

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 下午3:50
 */
object DeleteService : FileOperationService() {

    suspend fun delete(ctx: RoutingContext) {
        val block = AccessBlock.of(ctx)

        // 忽略请求头中的 Depth 属性
    }

}
