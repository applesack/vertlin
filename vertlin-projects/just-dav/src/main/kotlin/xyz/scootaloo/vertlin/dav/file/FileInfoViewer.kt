package xyz.scootaloo.vertlin.dav.file

import io.vertx.core.file.FileProps
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.dav.file.impl.FileInfoImpl
import xyz.scootaloo.vertlin.dav.file.impl.FilePathInfo
import xyz.scootaloo.vertlin.dav.service.FileOperationService
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/3 下午11:13
 */
object FileInfoViewer : FileOperationService() {

    private val log = X.getLogger(this::class)

    suspend fun traverse(
        point: String, depth: Int, deniedSet: Set<String>
    ): LinkedList<Pair<State, FileInfo>> {
        val receiver = LinkedList<Pair<State, FileInfo>>()
        when (depth) {
            0 -> traverse0(receiver, point, deniedSet)
            1 -> traverse1(receiver, point, deniedSet)
            else -> traverseInfinite(receiver, point, deniedSet)
        }
        return receiver
    }

    private suspend fun traverse0(
        receiver: LinkedList<Pair<State, FileInfo>>,
        point: String, deniedSet: Set<String>
    ): Boolean {
        return readFile(receiver, absolute(point).absolutePathString(), deniedSet)
    }

    private suspend fun traverse1(
        receiver: LinkedList<Pair<State, FileInfo>>,
        point: String, deniedSet: Set<String>
    ) {
        if (!traverse0(receiver, point, deniedSet)) {
            // 如果路径不存在或者访问终止, 则这里会直接短路
            return
        }

        val pointAbsolutePathString = absolute(point).absolutePathString()
        readDirectory(receiver, pointAbsolutePathString, deniedSet)
    }

    private suspend fun traverseInfinite(
        receiver: LinkedList<Pair<State, FileInfo>>,
        point: String, deniedSet: Set<String>
    ) {
        val candidates = LinkedList<String>()
        val pointAbsolutePathString = absolute(point).absolutePathString()
        candidates.add(pointAbsolutePathString)

        while (candidates.isNotEmpty()) {
            val next = candidates.removeFirst()
            val contents = readDirectory(receiver, next, deniedSet)
            if (contents.isNotEmpty()) {
                candidates.addAll(contents)
            }
        }
    }

    private suspend fun readDirectory(
        receiver: LinkedList<Pair<State, FileInfo>>,
        pointDirAbsolutePathString: String, deniedSet: Set<String>
    ): List<String> {
        // 如果文件夹路径不存在, 则接受器不会被触发
        val dirContents = runCatching { fs.readDir(pointDirAbsolutePathString).await() }
        if (dirContents.isFailure) {
            return emptyList()
        }

        val reduced = LinkedList<String>()
        for (pointAbsolutePathString in dirContents.getOrThrow()) {
            val props = runCatching { fs.props(pointAbsolutePathString).await() }
            if (props.isFailure) {
                simpleErrorProcess(receiver, props, pointAbsolutePathString)
            }

            val info = fileInfo(pointAbsolutePathString, props.getOrThrow())
            val state = if (info.path in deniedSet) State.FORBIDDEN else State.OK
            receiver.add(state to info)

            if (state == State.OK && info.isDirectory) {
                reduced.add(pointAbsolutePathString)
            }
        }
        return reduced
    }

    private suspend fun readFile(
        receiver: LinkedList<Pair<State, FileInfo>>,
        pointAbsolutePathString: String, deniedSet: Set<String>
    ): Boolean {
        val exists = runCatching { fs.exists(pointAbsolutePathString).await() }
        if (exists.isSuccess) {
            if (!exists.getOrThrow()) {
                simpleNotFoundProcess(receiver, pointAbsolutePathString)
                return false
            }
        } else {
            simpleErrorProcess(receiver, exists, pointAbsolutePathString)
            return false
        }

        val props = runCatching { fs.props(pointAbsolutePathString).await() }
        if (props.isFailure) {
            simpleErrorProcess(receiver, props, pointAbsolutePathString)
            return false
        }

        val info = fileInfo(pointAbsolutePathString, props.getOrThrow())
        val state = if (info.path in deniedSet) State.FORBIDDEN else State.OK
        receiver.add(state to info)
        return state == State.OK
    }

    private fun simpleNotFoundProcess(
        receiver: LinkedList<Pair<State, FileInfo>>, pointAbsolutePathString: String
    ) {
        val pathInfo = FilePathInfo(absolutePath, absolute(pointAbsolutePathString))
        receiver.add(State.NOT_FOUND to pathInfo)
    }

    private fun simpleErrorProcess(
        receiver: LinkedList<Pair<State, FileInfo>>,
        result: Result<Any>, pointAbsolutePathString: String
    ) {
        log.error("读取文件错误", result.exceptionOrNull())
        val pathInfo = FilePathInfo(absolutePath, absolute(pointAbsolutePathString))
        receiver.add(State.ERROR to pathInfo)
    }

    private fun fileInfo(pointAbsolutePathString: String, props: FileProps): FileInfo {
        val pointAbsolutePath = Path(pointAbsolutePathString).toAbsolutePath()
        return FileInfoImpl(absolutePath, pointAbsolutePath, props)
    }

}
