package xyz.scootaloo.vertlin.dav.util

import io.netty.handler.codec.http.HttpResponseStatus
import org.dom4j.DocumentHelper
import org.dom4j.Namespace
import org.dom4j.QName
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.util.MultiStatus.Reason.*

/**
 * @author flutterdash@qq.com
 * @since 2022/8/9 上午9:54
 */
object MultiStatus {

    fun buildResponses(events: List<Pair<Reason, String>>): String {
        val document = DocumentHelper.createDocument()
        val namespace = Namespace("D", "DAV:")
        val root = document.addElement(QName(Term.multiStatus, namespace))

        for ((reason, path) in events) {
            val resp = root.addElement(QName(Term.response, namespace))

            val href = resp.addElement(QName(Term.href, namespace))
            href.addText(PathUtils.encodeUriComponent(path))

            val status = resp.addElement(QName(Term.status, namespace))
            when (reason) {
                LOCKED -> {
                    status.addText(statusOf(StatusCode.locked))
                    val error = resp.addElement(QName(Term.error, namespace))
                    error.addElement(QName(Term.lockTokenSubmitted, namespace))
                }

                INTERNAL_ERROR -> status.addText(statusOf(StatusCode.internalError))
                CONFLICT -> status.addText(statusOf(StatusCode.conflict))
            }
        }

        return document.asXML()
    }

    fun statusOf(code: Int, version: String = "HTTP/1.1"): String {
        val details = HttpResponseStatus.valueOf(code)
        return "$version $code ${details.reasonPhrase()}"
    }

    enum class Reason {
        LOCKED, CONFLICT, INTERNAL_ERROR
    }

    private object Term {
        const val multiStatus = "multistatus"
        const val response = "response"
        const val href = "href"
        const val status = "status"
        const val error = "error"
        const val lockTokenSubmitted = "lock-token-submitted"
    }

}
