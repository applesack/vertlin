package xyz.scootaloo.vertlin.dav.service

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.Namespace
import org.dom4j.QName
import xyz.scootaloo.vertlin.boot.core.awaitParallelBlocking
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.ServerDefault
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.PropFindBlock
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.file.FileInfoViewer
import xyz.scootaloo.vertlin.dav.file.State
import xyz.scootaloo.vertlin.dav.lock.LockManager
import xyz.scootaloo.vertlin.dav.parse.header.DepthHeaderParser
import xyz.scootaloo.vertlin.dav.util.DateUtils
import xyz.scootaloo.vertlin.dav.util.PathUtils

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午4:32
 */
object PropFindService {

    private val lockManager by inject(LockManager::class)

    suspend fun propFind(ctx: RoutingContext) {
        val block = parseRequestContent(ctx)
        val deniedSet = Json.decodeFromString<List<String>>(lockManager.detect())
        val response = buildResponse(block, deniedSet)

        ctx.response().statusCode = StatusCode.multiStatus
        ctx.end(response)
    }

    private suspend fun parseRequestContent(ctx: RoutingContext): PropFindBlock {
        return awaitParallelBlocking {
            val headers = ctx.request().headers()
            val target = ctx.pathParam("*") ?: "/"
            val depth = DepthHeaderParser.parseDepth(headers.get(HttpHeaders.DEPTH)) ?: ServerDefault.depth
            PropFindBlock(target, depth)
        }
    }

    private suspend fun buildResponse(block: PropFindBlock, deniedSet: List<String>): String {
        val xml = DocumentHelper.createDocument()
        val namespace = Namespace("D", "DAV:")
        val root = xml.addElement(QName("multistatus", namespace))
        FileInfoViewer.traverse(block.target, block.depth.depth) { state, info ->
            buildResponseWithFileInfo(root, state, info, deniedSet)
        }
        return ""
    }

    private fun buildResponseWithFileInfo(
        root: Element, state: State, info: FileInfo, deniedSet: List<String>
    ): Boolean {
        if (info.path in deniedSet) {
            buildErrorResponseInMultiStatus(root, StatusCode.forbidden, info)
            return false
        }
        when (state) {
            State.OK -> buildOkResponseInMultiStatus(root, info)
            State.FORBIDDEN -> {}
            State.NOT_FOUND -> buildErrorResponseInMultiStatus(root, StatusCode.notFound, info)
            State.ERROR -> buildErrorResponseInMultiStatus(root, StatusCode.internalError, info)
        }

        return state == State.OK
    }

    private fun buildOkResponseInMultiStatus(root: Element, info: FileInfo) {
        val (response, namespace) = renderFileHref(root, info)

        val status = response.addElement(QName(MultiStatus.status))
        status.addText(statusOf(200))

        val propStat = response.addElement(QName(MultiStatus.propStat, namespace))

        val creationDate = propStat.addElement(QName(MultiStatus.creationDate, namespace))
        creationDate.addAttribute(MultiStatus.dateFormatMark, MultiStatus.datetimeTz)
        creationDate.addText(DateUtils.gmt(info.creationTime))

        val lastModified = propStat.addElement(QName(MultiStatus.lastModified, namespace))
        lastModified.addAttribute(MultiStatus.dateFormatMark, MultiStatus.datetimeRfc1123)

        val contentLength = propStat.addElement(QName(MultiStatus.contentLength, namespace))
        contentLength.addText(info.size.toString())

        val contentType = propStat.addElement(QName(MultiStatus.contentType, namespace))
        if (!info.isDirectory) {
            contentType.addText(info.mediaType)
        }

        val displayName = propStat.addElement(QName(MultiStatus.displayName, namespace))
        displayName.addText(info.filename)
    }

    private fun buildErrorResponseInMultiStatus(root: Element, code: Int, info: FileInfo) {
        val (response, namespace) = renderFileHref(root, info)
        val status = response.addElement(QName(MultiStatus.status, namespace))
        status.addText(statusOf(code))
    }

    private fun renderFileHref(root: Element, info: FileInfo): Pair<Element, Namespace> {
        val namespace = root.namespace
        val response = root.addElement(QName(MultiStatus.response, namespace))

        val href = response.addElement(QName(MultiStatus.href, namespace))
        href.addText(PathUtils.encodeUriComponent(info.path))

        return response to namespace
    }

    private fun statusOf(code: Int, version: String = "HTTP/1.1"): String {
        val details = HttpResponseStatus.valueOf(code)
        return "$version $code ${details.reasonPhrase()}"
    }

    object MultiStatus {
        const val response = "response"
        const val href = "href"
        const val propStat = "propstat"
        const val creationDate = "creationdate"
        const val contentLength = "getcontentlength"
        const val contentType = "getcontenttype"
        const val lastModified = "getlastmodified"
        const val displayName = "displayname"
        const val status = "status"

        const val dateFormatMark = "ns0:dt"
        const val datetimeTz = "dateTime.tz"
        const val datetimeRfc1123 = "rfc1123"
    }

}
