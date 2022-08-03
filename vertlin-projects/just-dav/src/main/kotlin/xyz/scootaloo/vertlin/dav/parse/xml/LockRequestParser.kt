package xyz.scootaloo.vertlin.dav.parse.xml

import org.dom4j.Document
import org.dom4j.DocumentHelper
import xyz.scootaloo.vertlin.dav.constant.Constant
import xyz.scootaloo.vertlin.dav.domain.LockBody

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午3:28
 */
object LockRequestParser {

    fun parseLockBody(content: String?): LockBody? {
        val result = runCatching { DocumentHelper.parseText(content) }
        if (result.isFailure) {
            return null
        }

        val document = result.getOrThrow()
        val isExclusive = handleLockScope(document)
        val owner = handleLockOwner(document)

        return LockBody(isExclusive, owner)
    }

    private fun handleLockScope(doc: Document): Boolean {
        return doc.selectSingleNode("/D:lockinfo/D:lockscope/D:exclusive") != null
    }

    private fun handleLockOwner(doc: Document): String {
        return doc.selectSingleNode("/D:lockinfo/D:owner")?.text ?: Constant.UNKNOWN
    }

}
