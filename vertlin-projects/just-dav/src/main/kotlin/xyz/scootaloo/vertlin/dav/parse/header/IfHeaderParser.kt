package xyz.scootaloo.vertlin.dav.parse.header

import xyz.scootaloo.vertlin.boot.core.like
import xyz.scootaloo.vertlin.dav.domain.ConditionGroup
import xyz.scootaloo.vertlin.dav.domain.ETagCondition
import xyz.scootaloo.vertlin.dav.domain.IfHeader
import xyz.scootaloo.vertlin.dav.domain.TokenCondition
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午11:12
 */
object IfHeaderParser {

    private val symbols = arrayOf('<', '(', '[', '>', ')', ']')

    fun parseIfCondition(content: String?): IfHeader? {
        val notnull = content ?: return null

        val stack = Stack<Pair<Char, Int>>()
        val conditions = ArrayList<ConditionGroup>()
        var conditionGroup = ConditionGroup()
        var tagged: String? = null

        for (pos in notnull.indices) {
            val ch = notnull[pos]
            val idx = symbols.indexOf(ch)
            if (idx < 0) {
                continue
            } else if (idx < 3) {
                stack.push(ch to idx)
            } else {
                if (stack.isEmpty()) {
                    return null
                }

                val (begin, beginIdx) = stack.pop()
                if (begin == symbols[idx - 3]) {
                    if (begin == '<' && stack.isEmpty()) {
                        tagged = notnull.substring(beginIdx + 1, pos)
                    } else if (begin == '(') {
                        conditions.add(conditionGroup)
                        conditionGroup = ConditionGroup()
                    } else if (begin == '<') {
                        val token = handleToken(notnull, beginIdx, pos) ?: return null
                        conditionGroup.tokenConditions.add(token)
                    } else if (begin == '[') {
                        val eTag = handleETag(notnull, beginIdx, pos) ?: return null
                        conditionGroup.eTagConditions.add(eTag)
                    }
                } else {
                    return null
                }
            }
        }

        return IfHeader(tagged, conditions)
    }

    private fun handleToken(content: String, begin: Int, end: Int): TokenCondition? {
        val rest = content.substring(begin + 1, end)
        if (rest.isBlank()) {
            return null
        }
        return TokenCondition(hasNotMark(content, begin), rest)
    }

    private fun handleETag(content: String, begin: Int, end: Int): ETagCondition? {
        val rest = content.substring(begin + 1, end)
        val eTag = ETagHeaderParser.parseETag(rest) ?: return null
        return ETagCondition(hasNotMark(content, begin), eTag)
    }

    private fun hasNotMark(content: String, pos: Int): Boolean {
        var whitePos = pos - 1
        while (whitePos > 2 && (content[whitePos] == ' ' || content[whitePos] == '\n')) {
            whitePos--
        }
        return (whitePos > 2 && content.substring(whitePos - 2, whitePos + 1) like "not")
    }

}
