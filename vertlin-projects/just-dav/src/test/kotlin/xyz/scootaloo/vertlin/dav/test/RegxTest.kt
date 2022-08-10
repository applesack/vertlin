package xyz.scootaloo.vertlin.dav.test

import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author flutterdash@qq.com
 * @since 2022/8/10 下午2:31
 */
class RegxTest : TestDSL {

    private val regx: Pattern = Pattern.compile("(\\w+)://([^/:]+)(:\\d*)?")

    @Test
    fun test() {
        groupOf("http://192.168.0.1:8080/abc").log()
        groupOf("http://scootaloo:8080/abc").log()
        groupOf("http://scootaloo/abc").log()
        groupOf("http://scootaloo/:8080abc").log()
        groupOf("http://8080abc").log()
        groupOf("/8080abc").log()
    }

    private fun groupOf(text: String): Group {
        val matcher = regx.matcher(text)
        return Group(matcher.find(), matcher)
    }

    class Group(
        private val isMatched: Boolean,
        private val matcher: Matcher
    ) {
        override fun toString(): String {
            val builder = buildString {
                append(isMatched)
                if (isMatched) {
                    append('\n')
                    append("full: ${matcher.group(0)}\n")
                    append("protocol: ${matcher.group(1)}\n")
                    append("host: ${matcher.group(2)}\n")
                    append("port: ${matcher.group(3)}\n")
                }
                append('\n')
            }
            return builder
        }
    }

}
