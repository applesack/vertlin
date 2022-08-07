package xyz.scootaloo.vertlin.dav.service

import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Encoder
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.AccessBlock
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.lock.LockManager
import xyz.scootaloo.vertlin.dav.util.ContextUtils
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 上午11:09
 */
object UploadService : FileOperationService() {

    private val log = X.getLogger(this::class)

    private val lockManager by inject(LockManager::class)

    suspend fun put(ctx: RoutingContext) {
        val block = AccessBlock.of(ctx)
        val detectPoint = Encoder.encode(Triple(block.target, block.condition, block.depth.depth))
        val deniedSet = lockManager.detect<List<String>>(detectPoint).toSet()

        val response = ctx.response()
        if (block.target in deniedSet) {
            response.statusCode = StatusCode.forbidden
            response.end()
            return
        }

        // 上传路径的父路径必须存在 (不能往一个不存在的文件夹里上传文件)
        // 上传路径恰好有一个存在的文件 (如果已经存在的文件是普通文件, 则普通文件被覆盖, 如果是文件夹则产生冲突)

        val targetFullPath = absolute(block.target).absolutePathString()
        val parentPath = FileInfo.parent(targetFullPath, absolutePathString)
        if (!parentPath.startsWith(absolutePathString)) {
            response.statusCode = StatusCode.conflict
            response.end()
            return
        }

        if (!fs.exists(parentPath).await() ||
            (fs.exists(targetFullPath).await() &&
                    fs.props(targetFullPath).await().isDirectory)
        ) {
            response.statusCode = StatusCode.conflict
            response.end()
            return
        }

        val content = ctx.body().buffer()
        val result = runCatching { upload(targetFullPath, content) }
        if (result.isFailure) {
            val msg = "上传文件错误: 文件名${block.target}, " +
                    "上传实际路径${targetFullPath}, 文件大小${content.length()}"
            log.error(msg, result.exceptionOrNull())
            ctx.fail(StatusCode.internalError)
            return
        }

        val username = ContextUtils.displayCurrentUserName(ctx)
        val msg = "上传文件: $username => ${block.target}"
        log.info(msg)

        response.end()
    }

    private suspend fun upload(absoluteTarget: String, content: Buffer) {
        fs.writeFile(absoluteTarget, content).await()
    }

}
