package xyz.scootaloo.vertlin.dav.parse.header

import xyz.scootaloo.vertlin.dav.domain.DepthHeader
import java.util.StringJoiner

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 上午12:07
 */
object DepthHeaderParser {

    fun parseDepth(content: String?): DepthHeader? {
        val notnull = content ?: return null

        var noRoot = false
        var depth = 0

        for (ch in notnull) {
            when (ch) {
                '0' -> depth = 0
                '1' -> depth = 1
                'y' -> depth = -1
                'r' -> noRoot = true
            }
        }

        return DepthHeader(depth, noRoot)
    }

}
