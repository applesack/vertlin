package xyz.scootaloo.vertlin.dav.service

import io.vertx.core.file.FileSystem
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.application.WebDAV
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 下午1:53
 */
abstract class FileOperationService {

    protected val fs by inject(FileSystem::class)
    protected val dav by inject(WebDAV::class)
    protected val absolutePath: Path by lazy { Path(dav.path).toAbsolutePath() }
    protected val absolutePathString: String by lazy { absolutePath.absolutePathString() }

    protected fun absolute(path: String): Path {
        return Path(absolutePathString, path).toAbsolutePath()
    }

}
