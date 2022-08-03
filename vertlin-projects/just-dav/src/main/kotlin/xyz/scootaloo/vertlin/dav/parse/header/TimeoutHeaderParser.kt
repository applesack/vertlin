package xyz.scootaloo.vertlin.dav.parse.header

import xyz.scootaloo.vertlin.boot.core.like
import xyz.scootaloo.vertlin.dav.domain.TimeoutHeader

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 上午12:02
 */
object TimeoutHeaderParser {

    fun parseTimeout(content: String?): TimeoutHeader? {
        val notnull = content ?: return null
        var infinite = false
        var amount = -1L
        for (segment in notnull.split(',')) {
            val rest = segment.trim()
            if (rest like "Infinite") {
                infinite = true
            } else if (rest.startsWith("Second-") && rest.length > 7) {
                val rsl = runCatching { rest.substring(7).toLong() }
                if (rsl.isFailure)
                    return null
                amount = rsl.getOrDefault(-1L)
            }
        }

        if (amount < 0)
            return null
        return TimeoutHeader(amount, infinite)
    }

}
