package xyz.scootaloo.vertlin.web

import io.vertx.core.http.impl.MimeMapping

/**
 * @author flutterdash@qq.com
 * @since 2022/8/6 下午5:00
 */
object ContentType {

    private val xmlType by lazy { MimeMapping.getMimeTypeForExtension("xml") }
    private const val defTransferCharset = "utf-8"

    const val CONTENT_TYPE = "Content-Type"

    fun xml(charset: String? = defTransferCharset): String {
        val notnullCharset = charset ?: defTransferCharset
        return "$xmlType; $notnullCharset"
    }

}
