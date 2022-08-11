package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.AccessBlock
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.util.ContextUtils
import xyz.scootaloo.vertlin.dav.util.FileOperations
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 下午3:15
 */
object CollectionService : FileOperations() {

    private val log = X.getLogger(this::class)

    suspend fun mkdir(ctx: RoutingContext) {
        val block = AccessBlock.of(ctx)

        // 创建文件夹时检查该文件夹的父文件夹是否存在, 如果不存在则冲突
        // 检查创建请求是否携带了请求体, 如果有, 则不处理并返回 415 不支持的媒体类型

        val targetFullPath = absolute(block.target).absolutePathString()
        val parentFullPath = FileInfo.parent(targetFullPath, absolutePathString)
        val parentRelativePath = FileInfo.relative(absolutePath, Path(parentFullPath))

        val deniedSet = detect(ctx, parentRelativePath, block.condition, 1) ?: return

        val response = ctx.response()
        if (parentRelativePath in deniedSet || block.target in deniedSet) {
            response.statusCode = StatusCode.forbidden
            response.end()
            return
        }

        if (!fs.exists(parentFullPath).await()) {
            response.statusCode = StatusCode.conflict
            response.end()
            return
        }

        if (!fs.props(parentFullPath).await().isDirectory) {
            response.statusCode = StatusCode.methodNotAllowed
            response.end()
            return
        }

        if (fs.exists(targetFullPath).await()) {
            response.statusCode = StatusCode.conflict
            response.end()
            return
        }

        val body = ctx.body().buffer()
        if (body != null) {
            response.statusCode = StatusCode.unsupportedMediaType
            response.end()
            return
        }

        val result = runCatching { createDirectory(targetFullPath) }
        if (result.isFailure) {
            log.error("创建目录失败", result.exceptionOrNull())
            return
        }

        val username = ContextUtils.displayCurrentUserName(ctx)
        val msg = "创建目录: $username => ${block.target}"
        log.info(msg)

        response.statusCode = StatusCode.created
        response.end()
    }

    private suspend fun createDirectory(targetAbsolutePath: String) {
        fs.mkdir(targetAbsolutePath).await()
    }

}
