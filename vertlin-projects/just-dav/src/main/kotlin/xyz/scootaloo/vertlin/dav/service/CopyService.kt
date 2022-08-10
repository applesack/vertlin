package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.file.copyOptionsOf
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.AccessBlock
import xyz.scootaloo.vertlin.dav.domain.DepthHeader
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.parse.header.DestinationHeaderParser
import xyz.scootaloo.vertlin.dav.parse.header.OverwriteHeaderParser
import xyz.scootaloo.vertlin.dav.util.ContextUtils
import xyz.scootaloo.vertlin.dav.util.FileDifference
import xyz.scootaloo.vertlin.dav.util.FileDifference.DiffParam
import xyz.scootaloo.vertlin.dav.util.FileDifference.Node
import xyz.scootaloo.vertlin.dav.util.FileDifference.Status.*
import xyz.scootaloo.vertlin.dav.util.MultiStatus
import xyz.scootaloo.vertlin.dav.util.MultiStatus.Reason
import xyz.scootaloo.vertlin.dav.util.PathUtils
import xyz.scootaloo.vertlin.web.endWithXml
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/8 下午3:38
 */
object CopyService : FileOperationService() {

    private val log = X.getLogger(this::class)

    suspend fun copy(ctx: RoutingContext) {
        val block = AccessBlock.of(ctx)

        // 只处理0和无限的情况, 如果提交深度为1, 则视为无限
        val depth = if (block.depth.depth != 1)
            block.depth.depth
        else
            DepthHeader.infinite

        // 构建参数, 同时确定源位置和目的位置都可以访问, 否则短路
        val param = buildCopyParam(ctx, block, depth) ?: return
        val events = copyFiles(ctx, param) ?: return

        val username = ContextUtils.displayCurrentUserName(ctx)
        log.info("复制文件: $username, ${param.source} => ${param.destination}")

        if (events.isEmpty()) {
            val response = ctx.response()
            response.statusCode = StatusCode.noContent
            response.end()
            return
        } else {
            val xml = MultiStatus.buildResponses(events)
            ctx.endWithXml(xml)
        }
    }

    private suspend fun buildCopyParam(ctx: RoutingContext, block: AccessBlock, depth: Int): DiffParam? {
        val headers = ctx.request().headers()
        val overwrite = OverwriteHeaderParser.parseOverwrite(headers[HttpHeaders.OVERWRITE], false)
        val destination = DestinationHeaderParser.parseDest(headers[HttpHeaders.DESTINATION]) ?: ""

        if (destination.isEmpty()) {
            // 缺失目的位置信息
            ctx.response().statusCode = StatusCode.badRequest
            ctx.response().end()
            return null
        }

        val sourceDeniedSet = detect(ctx, block, depth) ?: return null
        val destinationDeniedSet = detect(ctx, destination, block.condition, depth) ?: return null

        return DiffParam(
            block.target, destination,
            sourceDeniedSet, destinationDeniedSet,
            depth != 0, overwrite
        )
    }

    private suspend fun copyFiles(
        ctx: RoutingContext, param: DiffParam
    ): List<Pair<Reason, String>>? {
        // 目标路径不能直接是根目录
        if (param.destination.isEmpty() || param.destination == "/") {
            ctx.response().statusCode = StatusCode.badRequest
            ctx.response().end()
            return null
        }

        // 源路径和目的路径相同, 无需动作
        if (param.source == param.destination) {
            return emptyList()
        }

        // 检查目的路径的父容器是否存在 (如果存在, 则必须是文件夹, 只有文件夹才能存放文件)
        val destinationAbsolutePath = absolute(param.destination).absolutePathString()
        val absoluteDestParentPath = FileInfo.parent(destinationAbsolutePath, absolutePathString)
        if (!fs.exists(absoluteDestParentPath).await()) {
            ctx.response().statusCode = StatusCode.conflict
            ctx.response().end()
            return null
        }

        if (!fs.props(absoluteDestParentPath).await().isDirectory) {
            ctx.response().statusCode = StatusCode.conflict
            ctx.response().end()
            return null
        }

        // 如果目的路径是源路径的一个子集, 则将来源路径拷贝到目标路径会导致无限递归
        // 按照协议原则(尽可能多地完成复制操作), 将目标路径在复制时跳过
        val skipSet = HashSet<String>()
        if (param.destination.startsWith(param.source)) {
            skipSet.add(param.destination)
        }

        // 源路径必须存在, 否则复制操作无意义
        val sourceAbsolutePath = absolute(param.source).absolutePathString()
        if (!fs.exists(sourceAbsolutePath).await()) {
            ctx.response().statusCode = StatusCode.notFound
            ctx.response().end()
            return null
        }

        /*
        1. 如果源路径是文件, 直接复制; fastCopyFile
        2. 如果源文件是文件夹; deepCopyFiles
            2.1 目的文件不存在, 执行2.2.2
                PS: 需要考虑目的路径是源路径的一个子集以及锁的情况, 所以不能直接复制
            2.2 目的文件存在
                2.2.1 是文件, 冲突, 根据覆盖情况处理
                    不覆盖, 则冲突, 否则删掉已有的文件, 将源路径递归复制到目的路径
                2.2.2 是文件夹, 执行复杂复制
                    同时读源路径和目的路径, 取交集, 构建前缀树,
                    定位到目的路径, 递归执行复制操作
                    1. 如果一个文件在树中存在,
                        1.1 如果是被锁的文件, 跳过, 记录状态
                        1.2 其他情况, 按照是否覆盖, 决定是否跳过, 如果覆盖, 则删除源文件, 复制到目标路径
                    2. 直接复制
         */

        val sourceProps = fs.props(sourceAbsolutePath).await()
        return if (!sourceProps.isDirectory) {
            fastCopyFile(param, sourceAbsolutePath, destinationAbsolutePath)
        } else {
            deepCopyFiles(param, sourceAbsolutePath, destinationAbsolutePath, skipSet)
        }
    }

    private suspend fun fastCopyFile(
        param: DiffParam, sourceAbsolutePath: String,
        destAbsolutePath: String
    ): List<Pair<Reason, String>> {

        // 只考虑源文件是单个文件的情况

        val destExists = fs.exists(destAbsolutePath).await()
        if (!destExists) {
            val result = runCatching {
                fs.copy(sourceAbsolutePath, destAbsolutePath).await()
            }
            if (result.isFailure) {
                logCopyFailure(param.source, result.exceptionOrNull())
                return listOf(Reason.INTERNAL_ERROR to param.source)
            }
            return emptyList()
        }

        return if (param.overwrite) {
            val copyOptions = copyOptionsOf(
                atomicMove = false, replaceExisting = true, copyAttributes = true
            )
            val result = runCatching {
                fs.copy(sourceAbsolutePath, destAbsolutePath, copyOptions)
            }
            if (result.isFailure) {
                logCopyFailure(param.destination, result.exceptionOrNull())
                return listOf(Reason.INTERNAL_ERROR to param.destination)
            }
            emptyList()
        } else {
            listOf(Reason.CONFLICT to param.destination)
        }
    }

    private suspend fun deepCopyFiles(
        param: DiffParam,
        sourceAbsolutePath: String, destinationAbsolutePath: String,
        skipSet: Set<String>
    ): List<Pair<Reason, String>> {

        // 只处理源文件是文件夹的情况

        if (!param.isInfinite) {
            // 只拷贝一个文件夹, 不包括内容
            return fastCopyFile(param, sourceAbsolutePath, destinationAbsolutePath)
        }

        val intersection = FileDifference.intersect(param, skipSet)

        val results = LinkedList<Pair<Reason, String>>()
        if (intersection.isEmpty()) {
            copyRecursive(results, param, sourceAbsolutePath, destinationAbsolutePath)
            return results
        }

        return copyFilesRecursive(
            results, param, intersection,
            sourceAbsolutePath, destinationAbsolutePath
        )
    }

    private suspend fun copyFilesRecursive(
        results: LinkedList<Pair<Reason, String>>, param: DiffParam, intersection: Node,
        sourceAbsolutePath: String, destAbsolutePath: String
    ): List<Pair<Reason, String>> {
        val deque = LinkedList<Triple<Node, String, String>>()
        deque.add(Triple(intersection, sourceAbsolutePath, destAbsolutePath))

        while (deque.isNotEmpty()) {
            val (node, sourceDirAbsolutePath, destDirAbsolutePath) = deque.removeFirst()
            val sourceDirContents = fs.readDir(sourceDirAbsolutePath).await()
            for (member in sourceDirContents) {

                val filename = PathUtils.filename(member)
                if (filename in node) {

                    val problem = node[member]!!
                    when (problem.status) {
                        LOCKED -> recordingFailedOperation(results, member, Reason.LOCKED)
                        SKIP -> recordingFailedOperation(results, member, Reason.CONFLICT)
                        DUPLICATE -> copyOnDuplicate(results, member, param)
                        PLACEHOLDER -> {
                            // 一个节点是文件夹, 下面可能存在重复项, 继续递归
                            val destSubDirAbsolutePath = PathUtils.buildPath(destDirAbsolutePath, filename)
                            val triple = Triple(problem, member, destSubDirAbsolutePath)
                            deque.addLast(triple)
                        }
                    }

                } else {
                    // 执行深拷贝
                    val destSubDirAbsolutePath = PathUtils.buildPath(destDirAbsolutePath, filename)
                    copyRecursive(results, param, member, destSubDirAbsolutePath)
                }
            }
        }

        return results
    }

    private suspend fun copyOnDuplicate(
        receiver: MutableList<Pair<Reason, String>>, fullAbsolutePath: String,
        param: DiffParam
    ) {
        // 目的路径与源路径存在同名文件

        val baseAbsolutePath = absolute(param.source).absolutePathString()
        var subPath = fullAbsolutePath.substring(baseAbsolutePath.length + 1)
        subPath = FileInfo.normalize(subPath)

        val destRelativePath = concatPath(param.source, subPath)
        val destAbsolutePath = absolute(destRelativePath).absolutePathString()

        val failOn = FileInfo.relative(absolutePath, Path(fullAbsolutePath))
        if (param.overwrite) {
            // 如果使用覆盖, 则先递归删除目的路径
            val result = runCatching {
                fs.deleteRecursive(destAbsolutePath, true).await()
            }
            if (result.isFailure) {
                logCopyFailure(failOn, result.exceptionOrNull())
                receiver.add(Reason.INTERNAL_ERROR to failOn)
                return
            } else {
                // 然后使用递归将源路径的内容复制到目的路径
                val copyResult = runCatching {
                    fs.copyRecursive(fullAbsolutePath, destAbsolutePath, true).await()
                }
                if (copyResult.isFailure) {
                    logCopyFailure(destRelativePath, copyResult.exceptionOrNull())
                    receiver.add(Reason.INTERNAL_ERROR to destRelativePath)
                }
            }
        } else {
            // 源路径和目的路径同时存在, 但提交的选项没有指明覆盖导致的冲突
            receiver.add(Reason.CONFLICT to failOn)
        }
    }

    private fun recordingFailedOperation(
        receiver: MutableList<Pair<Reason, String>>,
        fullAbsolutePath: String, reason: Reason
    ) {
        val relative = FileInfo.relative(absolutePath, Path(fullAbsolutePath))
        receiver.add(reason to relative)
    }

    private suspend fun copyRecursive(
        stateReceiver: MutableList<Pair<Reason, String>>, param: DiffParam,
        sourceAbsolutePath: String, destAbsolutePath: String
    ) {
        // 检查目标文件是否存在, 如果目标文件存在而复制请求不要求重写, 则产生冲突
        val destExists = fs.exists(destAbsolutePath).await()
        if (destExists && !param.overwrite) {
            val relative = FileInfo.relative(Path(absolutePathString), Path(destAbsolutePath))
            stateReceiver.add(Reason.CONFLICT to relative)
            return
        }

        val result = runCatching {
            fs.copyRecursive(sourceAbsolutePath, destAbsolutePath, true).await()
        }

        if (result.isFailure) {
            val relative = FileInfo.relative(Path(absolutePathString), Path(destAbsolutePath))
            logCopyFailure(relative, result.exceptionOrNull())
            stateReceiver.add(Reason.INTERNAL_ERROR to relative)
        }
    }

    private fun logCopyFailure(path: String, error: Throwable?) {
        log.error("拷贝文件失败 => $path", error)
    }

}
