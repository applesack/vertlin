package xyz.scootaloo.vertlin.dav.util

import io.vertx.core.file.FileSystem
import io.vertx.ext.web.RoutingContext
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Encoder
import xyz.scootaloo.vertlin.dav.application.WebDAV
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.AccessBlock
import xyz.scootaloo.vertlin.dav.domain.IfHeader
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.lock.LockManager
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 下午1:53
 */
abstract class FileOperations {

    protected val fs by inject(FileSystem::class)
    protected val dav by inject(WebDAV::class)
    protected val absolutePath: Path by lazy { Path(dav.path).toAbsolutePath() }
    protected val absolutePathString: String by lazy { absolutePath.absolutePathString() }
    private val lockManager by inject(LockManager::class)

    protected suspend fun detect(ctx: RoutingContext, block: AccessBlock, depth: Int): List<String>? {
        return detect(ctx, block.target, block.condition, depth)
    }

    protected suspend fun detect(
        ctx: RoutingContext, path: String, condition: IfHeader?, depth: Int
    ): List<String>? {
        val params = Encoder.encode(Triple(path, condition, depth))
        val (allow, deniedSet) = lockManager.detect<Pair<Boolean, List<String>>>(params)
        if (!allow) {
            ctx.response().statusCode = StatusCode.forbidden
            ctx.end()
            return null
        }

        return deniedSet
    }

    protected fun absolute(path: String): Path {
        return Path(absolutePathString, path).toAbsolutePath()
    }

    protected fun relative(absolute: String): String {
        return FileInfo.relative(absolutePath, Path(absolute))
    }

    protected fun concatPath(a: String, b: String): String {
        val trimA = a.trim('/')
        val trimB = b.trim('/')
        val result = "$trimA/$trimB"
        return FileInfo.normalize(result)
    }

}
