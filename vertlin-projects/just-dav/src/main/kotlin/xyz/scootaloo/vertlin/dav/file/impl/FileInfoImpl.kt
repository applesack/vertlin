package xyz.scootaloo.vertlin.dav.file.impl

import io.vertx.core.file.FileProps
import io.vertx.core.http.impl.MimeMapping
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.file.FileInfo.Companion.filename
import xyz.scootaloo.vertlin.dav.file.FileInfo.Companion.mimeTypeOf
import xyz.scootaloo.vertlin.dav.file.FileInfo.Companion.relative
import java.nio.file.Path


/**
 * @author flutterdash@qq.com
 * @since 2022/8/3 下午11:36
 */
class FileInfoImpl(base: Path, current: Path, props: FileProps) : FileInfo {


    override val path = relative(base, current)
    override val filename = filename(path)
    override val creationTime = props.creationTime()
    override val lastAccessTime = props.lastAccessTime()
    override val lastModifiedTime = props.lastModifiedTime()
    override val isDirectory = props.isDirectory
    override val size = props.size()
    override val mediaType = mimeTypeOf(filename)

}
