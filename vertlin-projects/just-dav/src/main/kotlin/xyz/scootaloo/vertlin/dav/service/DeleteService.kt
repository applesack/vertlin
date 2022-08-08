package xyz.scootaloo.vertlin.dav.service

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import org.dom4j.DocumentHelper
import org.dom4j.Namespace
import org.dom4j.QName
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.AccessBlock
import xyz.scootaloo.vertlin.dav.util.ContextUtils
import xyz.scootaloo.vertlin.dav.util.PathUtils
import xyz.scootaloo.vertlin.web.endWithXml
import java.io.File
import java.util.*
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/7 下午3:50
 */
object DeleteService : FileOperationService() {

    private val log = X.getLogger(this::class)

    suspend fun delete(ctx: RoutingContext) {
        val block = AccessBlock.of(ctx)

        // 忽略请求头中的 Depth 属性, 任意delete请求的深度都视为infinite; 9.6.1
        val deniedSet = detect(ctx, block, 2) ?: return

        val targetAbsolutePath = absolute(block.target).absolutePathString()
        if (!fs.exists(targetAbsolutePath).await()) {
            ctx.response().statusCode = StatusCode.notFound
            ctx.end()
            return
        }

        val username = ContextUtils.displayCurrentUserName(ctx)
        log.info("删除文件: $username => ${block.target}")

        return if (!fs.props(targetAbsolutePath).await().isDirectory || deniedSet.isEmpty()) {
            buildResponse(ctx, fastDeleteSingleFile(targetAbsolutePath))
        } else {
            buildResponse(ctx, deleteRecursiveWithDeniedSet(targetAbsolutePath, deniedSet))
        }
    }

    private suspend fun fastDeleteSingleFile(
        singleFileAbsolutePath: String
    ): List<Pair<Reason, String>> {
        val result = runCatching { fs.delete(singleFileAbsolutePath).await() }
        if (result.isFailure) {
            log.error("删除文件失败, 路径$singleFileAbsolutePath", result.exceptionOrNull())
            return listOf(Reason.INTERNAL_ERROR to relative(singleFileAbsolutePath))
        }
        return emptyList()
    }

    private suspend fun deleteRecursiveWithDeniedSet(
        targetRelativePath: String, deniedSet: List<String>
    ): List<Pair<Reason, String>> {
        val treeRoot = buildTrieTree(targetRelativePath, deniedSet)
        val queue = LinkedList<Selector>()
        val targetAbsolutePath = absolute(targetRelativePath).absolutePathString()

        // [targetRelativePath] 不可能正在被使用, 判断逻辑在 [detect] 中完成

        queue.addLast(Selector(targetAbsolutePath, treeRoot))

        val results = LinkedList<Pair<Reason, String>>()
        while (queue.isNotEmpty()) {
            val (absoluteDirPath, node) = queue.removeFirst()
            if (!fs.exists(absoluteDirPath).await()) {
                continue
            }

            val dirContents = fs.readDir(absoluteDirPath).await()
            for (subFileAbsolute in dirContents) {
                val filename = filenameFromAbsolutePath(subFileAbsolute)
                if (filename !in node) {
                    val result = runCatching {
                        fs.deleteRecursive(subFileAbsolute, true).await()
                    }
                    if (result.isFailure) {
                        log.error("删除文件失败, 路径$subFileAbsolute", result.exceptionOrNull())
                        results.add(Reason.INTERNAL_ERROR to relative(subFileAbsolute))
                    }
                    continue
                }

                val selectedNode = node[filename]!!
                if (selectedNode.end) {
                    results.add(Reason.LOCKED to relative(subFileAbsolute))
                    continue
                }

                val props = fs.props(subFileAbsolute).await()
                if (props.isDirectory) {
                    queue.addLast(Selector(subFileAbsolute, selectedNode))
                } else {
                    val result = runCatching { fs.delete(subFileAbsolute).await() }
                    if (result.isFailure) {
                        results.add(Reason.INTERNAL_ERROR to relative(subFileAbsolute))
                    }
                }
            }
        }

        return results
    }

    private fun buildTrieTree(relativeBasePath: String, used: List<String>): Node {
        val root = Node()
        for (use in used) {
            val relative = use.substring(relativeBasePath.length)
            val items = relative.split('/')
            var current = root
            for (item in items) {
                if (item in current) {
                    current = current[item]!!
                } else {
                    val next = Node()
                    current[item] = next
                    current = next
                }
            }
            current.end = true
        }
        return root
    }

    private fun buildResponse(ctx: RoutingContext, results: List<Pair<Reason, String>>) {
        val response = ctx.response()
        if (results.isEmpty()) {
            response.statusCode = StatusCode.noContent
            response.end()
            return
        }

        val xml = DocumentHelper.createDocument()
        val namespace = Namespace("D", "DAV:")
        val root = xml.addElement(QName(MultiStatus.multiStatus, namespace))

        for ((reason, path) in results) {
            val resp = root.addElement(QName(MultiStatus.response, namespace))

            val href = resp.addElement(QName(MultiStatus.href, namespace))
            href.addText(PathUtils.encodeUriComponent(path))

            val status = resp.addElement(QName(MultiStatus.status, namespace))
            when (reason) {
                Reason.LOCKED -> {
                    status.addText(statusOf(StatusCode.locked))
                    val error = resp.addElement(QName(MultiStatus.error, namespace))
                    error.addElement(QName(MultiStatus.lockTokenSubmitted, namespace))
                }

                Reason.INTERNAL_ERROR -> status.addText(statusOf(StatusCode.internalError))
            }
        }

        response.statusCode = StatusCode.multiStatus
        ctx.endWithXml(xml.asXML())
    }

    private fun statusOf(code: Int, version: String = "HTTP/1.1"): String {
        val details = HttpResponseStatus.valueOf(code)
        return "$version $code ${details.reasonPhrase()}"
    }

    private val separator by lazy { File.separatorChar }
    private fun filenameFromAbsolutePath(absolute: String): String {
        val idx = absolute.lastIndexOf(separator)
        if (idx < 0 || absolute.length - 1 == idx) return ""
        return absolute.substring(idx + 1)
    }

    private class Node(
        var end: Boolean = false,
        val subNodes: HashMap<String, Node> = HashMap()
    ) : MutableMap<String, Node> by subNodes

    private data class Selector(
        val absolutePath: String,
        val node: Node
    )

    private enum class Reason {
        LOCKED, INTERNAL_ERROR
    }

    private object MultiStatus {
        const val multiStatus = "multistatus"
        const val response = "response"
        const val href = "href"
        const val status = "status"
        const val error = "error"
        const val lockTokenSubmitted = "lock-token-submitted"
    }

}
