package xyz.scootaloo.vertlin.dav.file.impl

import xyz.scootaloo.vertlin.dav.file.FileInfo
import java.nio.file.Path

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 上午11:35
 */
class FilePathInfo(base: Path, current: Path) : FileInfo {

    override val path = FileInfo.relative(base, current)
    override val filename = FileInfo.filename(path)

    override val creationTime get() = throw UnsupportedOperationException()
    override val lastAccessTime get() = throw UnsupportedOperationException()
    override val lastModifiedTime get() = throw UnsupportedOperationException()
    override val isDirectory get() = throw UnsupportedOperationException()
    override val size get() = throw UnsupportedOperationException()
    override val mediaType get() = throw UnsupportedOperationException()

}
