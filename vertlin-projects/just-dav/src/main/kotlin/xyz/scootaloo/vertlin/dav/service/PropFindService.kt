package xyz.scootaloo.vertlin.dav.service

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.Namespace
import org.dom4j.QName
import xyz.scootaloo.vertlin.boot.core.awaitParallelBlocking
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Encoder
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
import xyz.scootaloo.vertlin.web.endWithXml

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午4:32
 */
object PropFindService {

    private val lockManager by inject(LockManager::class)

    suspend fun propFind(ctx: RoutingContext) {
        val block = parseRequestContent(ctx)
        val detectPoint = Encoder.encode(Pair(block.target, block.depth.depth))
        val deniedSet = lockManager.detect<List<String>>(detectPoint)
        val xmlResponse = buildResponse(block, deniedSet)

        ctx.response().statusCode = StatusCode.multiStatus
        ctx.endWithXml(xmlResponse)
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
        val files = FileInfoViewer.traverse(block.target, block.depth.depth, deniedSet.toSet())
        if (block.depth.noRoot && files.isNotEmpty()) {
            files.removeFirst()
        }

        val xml = DocumentHelper.createDocument()
        val namespace = Namespace("D", "DAV:")
        val root = xml.addElement(QName("multistatus", namespace))
        for ((state, file) in files) {
            buildResponseWithFileInfo(root, state, file)
        }

        return xml.asXML()
    }

    private fun buildResponseWithFileInfo(
        root: Element, state: State, info: FileInfo
    ) {
        when (state) {
            State.OK -> buildOkResponseInMultiStatus(root, info)
            State.FORBIDDEN -> buildErrorResponseInMultiStatus(root, StatusCode.forbidden, info)
            State.NOT_FOUND -> buildErrorResponseInMultiStatus(root, StatusCode.notFound, info)
            State.ERROR -> buildErrorResponseInMultiStatus(root, StatusCode.internalError, info)
        }
    }

    private fun buildOkResponseInMultiStatus(root: Element, info: FileInfo) {
        val (propStat, namespace) = renderFileHref(root, info)

        val status = propStat.addElement(QName(MultiStatus.status))
        status.addText(statusOf(200))

        val prop = propStat.addElement(QName(MultiStatus.prop, namespace))

        val creationDate = prop.addElement(QName(MultiStatus.creationDate, namespace))
        creationDate.addAttribute(MultiStatus.dateFormatMark, MultiStatus.datetimeTz)
        creationDate.addText(DateUtils.gmt(info.creationTime))

        val lastModified = prop.addElement(QName(MultiStatus.lastModified, namespace))
        lastModified.addAttribute(MultiStatus.dateFormatMark, MultiStatus.datetimeRfc1123)
        lastModified.addText(DateUtils.rfc1123(info.lastModifiedTime))

        val contentLength = prop.addElement(QName(MultiStatus.contentLength, namespace))
        contentLength.addText(info.size.toString())

        val resourceType = prop.addElement(QName(MultiStatus.resourceType, namespace))
        val contentType = prop.addElement(QName(MultiStatus.contentType, namespace))
        if (!info.isDirectory) {
            contentType.addText(info.mediaType)
        } else {
            resourceType.addElement(QName(MultiStatus.collection, namespace))
            contentType.addText(MultiStatus.unixDir)
        }

        val displayName = prop.addElement(QName(MultiStatus.displayName, namespace))
        displayName.addText(info.filename)
    }

    private fun buildErrorResponseInMultiStatus(root: Element, code: Int, info: FileInfo) {
        val (propStat, namespace) = renderFileHref(root, info)
        val status = propStat.addElement(QName(MultiStatus.status, namespace))
        status.addText(statusOf(code))

        val respText = when (code) {
            403 -> "Forbidden"
            404 -> "File Not Found"
            else -> "Server Internal Error"
        }
        val respDesc = propStat.addElement(QName(MultiStatus.responseDescription, namespace))
        respDesc.addText(respText)
    }

    // propStat, namespace
    private fun renderFileHref(root: Element, info: FileInfo): Pair<Element, Namespace> {
        val namespace = root.namespace
        val response = root.addElement(QName(MultiStatus.response, namespace))
        val propStat = response.addElement(QName(MultiStatus.propStat, namespace))

        val href = propStat.addElement(QName(MultiStatus.href, namespace))
        href.addText(PathUtils.encodeUriComponent(info.path))

        return propStat to namespace
    }

    private fun statusOf(code: Int, version: String = "HTTP/1.1"): String {
        val details = HttpResponseStatus.valueOf(code)
        return "$version $code ${details.reasonPhrase()}"
    }

    private object MultiStatus {
        const val response = "response"
        const val href = "href"
        const val propStat = "propstat"
        const val prop = "prop"
        const val creationDate = "creationdate"
        const val contentLength = "getcontentlength"
        const val contentType = "getcontenttype"
        const val lastModified = "getlastmodified"
        const val displayName = "displayname"
        const val status = "status"
        const val responseDescription = "responsedescription"
        const val resourceType = "resourcetype"
        const val collection = "collection"

        const val dateFormatMark = "ns0:dt"
        const val datetimeTz = "dateTime.tz"
        const val datetimeRfc1123 = "rfc1123"

        const val unixDir = "httpd/unix-directory"
    }

}
