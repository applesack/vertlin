package xyz.scootaloo.vertlin.dav.parse.header

/**
 * @author flutterdash@qq.com
 * @since 2022/8/8 下午5:57
 */
object OverwriteHeaderParser {

    fun parseOverwrite(content: String?, def: Boolean): Boolean {
        val notnull = content ?: return def
        return 'T' in notnull || 't' in notnull
    }

}
