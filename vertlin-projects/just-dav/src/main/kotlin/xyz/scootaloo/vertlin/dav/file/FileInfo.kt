package xyz.scootaloo.vertlin.dav.file

import io.vertx.core.http.impl.MimeMapping
import xyz.scootaloo.vertlin.dav.util.PathUtils
import java.io.File
import java.nio.file.Path

/**
 * @author flutterdash@qq.com
 * @since 2022/8/3 下午11:13
 */
interface FileInfo {

    val path: String

    val filename: String

    val creationTime: Long

    val lastAccessTime: Long

    val lastModifiedTime: Long

    val isDirectory: Boolean

    val size: Long

    val mediaType: String

    companion object {

        fun relative(base: Path, current: Path): String {
            return normalize(base.relativize(current).toString())
        }

        fun filename(path: String): String {
            val decoded = PathUtils.decodeUriComponent(path)
            val separatorIdx = decoded.lastIndexOf('/')
            if (separatorIdx == decoded.length - 1) {
                return ""
            }
            return decoded.substring(separatorIdx + 1)
        }

        fun parent(absolute: String, base: String): String {
            return File(absolute).parent ?: base
        }

        fun mimeTypeOf(filename: String): String {
            return (if (filename.lastIndexOf('.') > 0) {
                MimeMapping.getMimeTypeForFilename(filename)
            } else {
                MimeMapping.getMimeTypeForExtension(filename)
            }) ?: MimeMapping.getMimeTypeForExtension("bin")!!
        }

        fun normalize(path: String): String {
            val size = if (path.startsWith('/')) path.length else path.length + 1
            val builder = StringBuilder(size)
            if (!path.startsWith('/')) {
                builder.append('/')
            }
            for (ch in path) {
                if (ch == '\\') {
                    builder.append('/')
                } else {
                    builder.append(ch)
                }
            }
            if (builder.length > 1 && builder.last() == '/') {
                builder.setLength(builder.length - 1)
            }
            return builder.toString()
        }

    }

}
