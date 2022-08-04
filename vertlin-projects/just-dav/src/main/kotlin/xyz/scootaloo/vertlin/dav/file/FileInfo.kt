package xyz.scootaloo.vertlin.dav.file

import io.vertx.core.http.impl.MimeMapping
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
            val separatorIdx = path.lastIndexOf('/')
            if (separatorIdx == path.length - 1) {
                return ""
            }
            return path.substring(separatorIdx + 1)
        }

        fun mimeTypeOf(filename: String): String {
            return (if (filename.lastIndexOf('.') > 0) {
                MimeMapping.getMimeTypeForFilename(filename)
            } else {
                MimeMapping.getMimeTypeForExtension(filename)
            }) ?: MimeMapping.getMimeTypeForExtension("bin")!!
        }

        private fun normalize(path: String): String {
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
            return builder.toString()
        }

    }

}