package xyz.scootaloo.vertlin.dav.util

import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.AccessBlock
import xyz.scootaloo.vertlin.dav.domain.DepthHeader
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.parse.header.DestinationHeaderParser
import xyz.scootaloo.vertlin.dav.parse.header.OverwriteHeaderParser
import xyz.scootaloo.vertlin.dav.util.FileDifference.DiffParam
import xyz.scootaloo.vertlin.dav.util.FileDifference.Status.*
import xyz.scootaloo.vertlin.dav.util.MultiStatus.Reason
import xyz.scootaloo.vertlin.web.endWithXml
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * 提取 copy 和 move 的通用处理逻辑
 *
 * @author flutterdash@qq.com
 * @since 2022/8/10 下午11:47
 */
abstract class DualFileOperations : FileOperations() {

    private val log = X.getLogger(this::class)

    abstract fun actionName(): String

    /**
     * 操作单个文件
     *
     * 如果文件存在子结构(目录), 则操作对象也只有目录本身, 不包括它的子项,
     *
     * 默认行为, 如果[to]文件存在, 则必须使用[from]文件覆盖
     */
    abstract suspend fun action(from: String, to: String)

    abstract suspend fun actionRecursive(from: String, to: String)

    suspend fun handle(ctx: RoutingContext) {
        val block = AccessBlock.of(ctx)
        val depth = calculateOperationDepth(block)
        val param = buildDiffParam(ctx, block, depth) ?: return

        val events = handleDualFiles(ctx, param) ?: return

        val username = ContextUtils.displayCurrentUserName(ctx)
        log.info("${actionName()}: $username from ${param.source} to ${param.destination}")

        val response = ctx.response()
        if (events.isEmpty()) {
            response.statusCode = if (events.state == OperateState.NO_CONTENT) {
                StatusCode.noContent
            } else {
                StatusCode.created
            }
            ctx.end()
            return
        }

        ctx.endWithXml(MultiStatus.buildResponses(events.multi))
    }

    /**
     * 计算操作的实际深度
     *
     * 操作深度只能取两种值, 0, 操作目标文件; infinite, 操作目标文件及其所有子文件;
     * 如果输入为1, 则视为infinite
     */
    private fun calculateOperationDepth(block: AccessBlock): Int {
        return if (block.depth.depth != 1)
            block.depth.depth
        else
            DepthHeader.infinite
    }

    /**
     * 构建基本参数, 这个参数可用于操作两个不同位置的文件时提供必要的信息
     *
     * 需要注意, 操作两个不同位置的文件的前提是需要具备访问这两个位置的权限, 如果没有其中任意一个位置的权限, 则操作无法完成;
     * 如果出现上述情况, 则返回`null`值, 并且对请求执行了默认处理
     */
    private suspend fun buildDiffParam(
        ctx: RoutingContext, block: AccessBlock, depth: Int
    ): DiffParam? {
        val headers = ctx.request().headers()
        val overwrite = OverwriteHeaderParser.parseOverwrite(headers[HttpHeaders.OVERWRITE], false)
        val destination = DestinationHeaderParser.parseDest(headers[HttpHeaders.DESTINATION]) ?: ""

        if (destination.isEmpty()) {
            // 缺失目的位置信息
            return terminate(ctx, StatusCode.badRequest)
        }

        val sourceDeniedSet = detect(ctx, block, depth) ?: return terminate(ctx, StatusCode.locked)
        val destinationDeniedSet =
            detect(ctx, destination, block.condition, depth) ?: return terminate(ctx, StatusCode.locked)

        return DiffParam(
            block.target, destination,
            sourceDeniedSet, destinationDeniedSet,
            depth != 0, overwrite
        )
    }

    private suspend fun handleDualFiles(
        ctx: RoutingContext, param: DiffParam
    ): ResponseState? {
        // 目标路径不能直接是根目录
        if (param.destination.isEmpty() || param.destination == "/") {
            ctx.response().statusCode = StatusCode.badRequest
            ctx.response().end()
            return null
        }

        val state = ResponseState(OperateState.NO_CONTENT)

        // 源路径和目的路径相同, 无需动作
        if (param.source == param.destination) {
            return state
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
        // todo 如果情况相反, 源路径是目的路径的子集, 应该如何处理?
        val skipSet = HashSet<String>()
        // TIPS#1: /test是/test1的前缀, 但是/test1不是/test的子集, 它们共享同一个父路径
        if (param.destination.startsWith(param.source + "/")) {
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
            fastProcessFile(state, param, sourceAbsolutePath, destinationAbsolutePath)
        } else {
            deepProcessFiles(state, param, skipSet, sourceAbsolutePath, destinationAbsolutePath)
        }
    }

    private suspend fun fastProcessFile(
        receiver: ResponseState, param: DiffParam,
        sourceAbsolutePath: String, destAbsolutePath: String
    ): ResponseState {

        // 只考虑源文件是单个文件的情况

        val destExists = fs.exists(destAbsolutePath).await()
        if (!destExists) {
            val result = runCatching {
                action(sourceAbsolutePath, destAbsolutePath)
            }
            if (result.isFailure) {
                logOperationFailure(param.source, result.exceptionOrNull())
                receiver.addState(Reason.INTERNAL_ERROR, param.source)
            }
            return receiver
        }

        if (param.overwrite) {
            val result = runCatching {
                action(sourceAbsolutePath, destAbsolutePath)
            }
            if (result.isFailure) {
                logOperationFailure(param.destination, result.exceptionOrNull())
                receiver.addState(Reason.INTERNAL_ERROR, param.destination)
            }
        } else {
            receiver.addState(Reason.CONFLICT, param.destination)
        }

        return receiver
    }

    private suspend fun deepProcessFiles(
        receiver: ResponseState, param: DiffParam, skipSet: Set<String>,
        sourceAbsolutePath: String, destinationAbsolutePath: String
    ): ResponseState {

        // 只处理源文件是目录的情况

        if (!param.isInfinite) {
            // 只拷贝一个文件夹, 不包括内容
            return fastProcessFile(receiver, param, sourceAbsolutePath, destinationAbsolutePath)
        }

        val intersection = FileDifference.intersect(param, skipSet)

        if (intersection.isEmpty()) {
            simpleProcessFileRecursive(receiver, param, sourceAbsolutePath, destinationAbsolutePath)
            return receiver
        }

        matchAndReplaceFilesRecursive(
            receiver, param, intersection, sourceAbsolutePath, destinationAbsolutePath
        )

        return receiver
    }

    private suspend fun simpleProcessFileRecursive(
        receiver: ResponseState, param: DiffParam,
        sourceAbsolutePath: String, destAbsolutePath: String
    ) {
        // 检查目标文件是否存在, 如果目标文件存在而复制请求不要求重写, 则产生冲突

        val destExists = fs.exists(destAbsolutePath).await()
        if (destExists && !param.overwrite) {
            val relative = FileInfo.relative(Path(absolutePathString), Path(destAbsolutePath))
            receiver.addState(Reason.CONFLICT, relative)
            return
        }

        val result = runCatching {
            actionRecursive(sourceAbsolutePath, destAbsolutePath)
        }

        if (result.isFailure) {
            val relative = FileInfo.relative(Path(absolutePathString), Path(destAbsolutePath))
            logOperationFailure(relative, result.exceptionOrNull())
            receiver.addState(Reason.INTERNAL_ERROR, relative)
        }
    }

    private suspend fun matchAndReplaceFilesRecursive(
        receiver: ResponseState, param: DiffParam, intersection: FileDifference.Node,
        sourceAbsolutePath: String, destAbsolutePath: String
    ) {
        // @TIPS#1: 考虑下面这种情况
        // 从/test 拷贝到/test/a/test, /test/a存在, 但是/test/a/test不存在
        // 这种情况是允许执行复制操作的, 所以需要先创建出/test/a/test

        if (!fs.exists(destAbsolutePath).await()) {
            val result = runCatching { fs.copy(sourceAbsolutePath, destAbsolutePath).await() }
            if (result.isFailure) {
                // 创建文件夹失败
                logOperationFailure(destAbsolutePath, result.exceptionOrNull())
                receiver.addState(Reason.INTERNAL_ERROR, param.destination)
                return
            }
        }

        val deque = LinkedList<Triple<FileDifference.Node, String, String>>()
        deque.add(Triple(intersection, sourceAbsolutePath, destAbsolutePath))

        while (deque.isNotEmpty()) {
            val (node, sourceDirAbsolutePath, destDirAbsolutePath) = deque.removeFirst()
            val sourceDirContents = fs.readDir(sourceDirAbsolutePath).await()
            for (member in sourceDirContents) {

                val filename = PathUtils.filename(member)
                if (filename in node) {

                    val problem = node[filename]!!
                    when (problem.status) {
                        LOCKED -> recordingFailedOperation(receiver, member, Reason.LOCKED)
                        SKIP -> recordingFailedOperation(receiver, member, Reason.CONFLICT)
                        DUPLICATE -> processOnDuplicate(receiver, member, param)
                        PLACEHOLDER -> {
                            // 一个节点是文件夹, 下面可能存在重复项, 继续递归
                            val destSubDirAbsolutePath = PathUtils.buildPath(destDirAbsolutePath, filename)
                            val triple = Triple(problem, member, destSubDirAbsolutePath)
                            deque.addLast(triple)
                        }
                    }

                } else {
                    val destSubDirAbsolutePath = PathUtils.buildPath(destDirAbsolutePath, filename)
                    simpleProcessFileRecursive(receiver, param, member, destSubDirAbsolutePath)
                }
            }
        }
    }

    private suspend fun processOnDuplicate(
        receiver: ResponseState, fullAbsolutePath: String, param: DiffParam
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
                logOperationFailure(failOn, result.exceptionOrNull())
                receiver.addState(Reason.INTERNAL_ERROR, failOn)
                return
            } else {
                // 然后使用递归将源路径的内容复制到目的路径
                val processResult = runCatching {
                    actionRecursive(fullAbsolutePath, destAbsolutePath)
                }
                if (processResult.isFailure) {
                    logOperationFailure(destRelativePath, processResult.exceptionOrNull())
                    receiver.addState(Reason.INTERNAL_ERROR, destRelativePath)
                    return
                }
                // 如果有文件被覆盖, 则更新状态码
                receiver.state = OperateState.NO_CONTENT
            }
        } else {
            // 源路径和目的路径同时存在, 但提交的选项没有指明覆盖导致的冲突
            receiver.addState(Reason.CONFLICT, failOn)
        }
    }

    private fun <T> terminate(ctx: RoutingContext, code: Int): T? {
        ctx.response().statusCode = code
        ctx.response().end()
        return null
    }

    private fun recordingFailedOperation(
        receiver: ResponseState, fullAbsolutePath: String, reason: Reason
    ) {
        val relative = FileInfo.relative(absolutePath, Path(fullAbsolutePath))
        receiver.addState(reason, relative)
    }

    private fun logOperationFailure(dest: String, error: Throwable?) {
        log.error("${actionName()}失败: $dest", error)
    }

    private class ResponseState(
        var state: OperateState = OperateState.CREATED,
        val multi: MutableList<Pair<Reason, String>> = LinkedList()
    ) : List<Pair<Reason, String>> by multi {
        fun addState(reason: Reason, path: String) {
            multi.add(reason to path)
        }
    }

    private enum class OperateState {
        CREATED, NO_CONTENT
    }

}
