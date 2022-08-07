package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.FileSystemAccess
import io.vertx.ext.web.handler.StaticHandler
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.application.WebDAV
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/6 下午11:10
 */
object StaticService {

    private val webdav by inject(WebDAV::class)
    private val staticResourceRoot by lazy { Path(webdav.path).absolutePathString() }
    private val staticHandler by lazy {
        StaticHandler
            .create(FileSystemAccess.ROOT, staticResourceRoot)
            .setAlwaysAsyncFS(true)
            .setCachingEnabled(true)
            .setEnableFSTuning(true)
            .setEnableRangeSupport(true)
    }

    fun get(ctx: RoutingContext) {
        staticHandler.handle(ctx)
    }

}
