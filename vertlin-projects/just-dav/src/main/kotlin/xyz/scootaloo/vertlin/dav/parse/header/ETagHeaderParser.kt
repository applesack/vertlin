package xyz.scootaloo.vertlin.dav.parse.header

import xyz.scootaloo.vertlin.dav.domain.ETag

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午11:26
 */
object ETagHeaderParser {

    fun parseETag(content: String?): ETag? {
        val notnull = content ?: return null
        var rest = notnull.trim()
        val weak = rest.startsWith("\\w", true)
        rest = if (weak) rest.substring(2) else rest
        if (rest.startsWith("\"")) {
            rest = rest.trim('"')
        }

        if (rest.isBlank())
            return null
        return ETag(weak, rest)
    }

}
