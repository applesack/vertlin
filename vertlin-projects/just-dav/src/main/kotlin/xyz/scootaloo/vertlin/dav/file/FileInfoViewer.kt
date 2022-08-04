package xyz.scootaloo.vertlin.dav.file

import io.vertx.core.file.FileProps
import io.vertx.core.file.FileSystem
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.application.WebDAV
import xyz.scootaloo.vertlin.dav.file.impl.FileInfoImpl
import xyz.scootaloo.vertlin.dav.file.impl.FilePathInfo
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/3 下午11:13
 */
object FileInfoViewer {

    private val log = X.getLogger(this::class)

    private val fs by inject(FileSystem::class)
    private val dav by inject(WebDAV::class)
    private val absolutePath: Path by lazy { Path(dav.path).toAbsolutePath() }
    private val absolutePathString: String by lazy { absolutePath.absolutePathString() }

    suspend fun traverse(point: String, depth: Int, receiver: FileInfoReceiver) {
        when (depth) {
            0 -> traverse0(point, receiver)
            1 -> traverse1(point, receiver)
            else -> traverseInfinite(point, receiver)
        }
    }

    private suspend fun traverse0(point: String, receiver: FileInfoReceiver): Boolean {
        return readFile(absolute(point).absolutePathString(), false, receiver)
    }

    private suspend fun traverse1(point: String, receiver: FileInfoReceiver) {
        if (!traverse0(point, receiver)) {
            // 如果路径不存在或者访问终止, 则这里会直接短路
            return
        }

        val pointAbsolutePathString = Path(point).absolutePathString()
        readDirectory(pointAbsolutePathString, receiver)
    }

    private suspend fun traverseInfinite(
        point: String, receiver: FileInfoReceiver
    ) {
        val candidates = LinkedList<String>()
        val pointAbsolutePathString = absolute(point).absolutePathString()
        candidates.add(pointAbsolutePathString)

        while (candidates.isNotEmpty()) {
            val next = candidates.removeFirst()
            val contents = readDirectory(next, receiver)
            if (contents.isNotEmpty()) {
                candidates.addAll(contents)
            }
        }
    }

    private suspend fun readDirectory(
        pointDirAbsolutePathString: String, receiver: FileInfoReceiver
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
                simpleErrorProcess(props, receiver, pointAbsolutePathString)
            }

            val info = fileInfo(pointAbsolutePathString, props.getOrThrow())
            if (receiver.receive(State.OK, info) && props.getOrThrow().isDirectory) {
                reduced.add(pointAbsolutePathString)
            }
        }
        return reduced
    }

    private suspend fun readFile(
        pointAbsolutePathString: String, existsChecked: Boolean, receiver: FileInfoReceiver
    ): Boolean {
        if (!existsChecked) {
            val exists = runCatching { fs.exists(pointAbsolutePathString).await() }
            if (exists.isSuccess) {
                if (!exists.getOrThrow()) {
                    return simpleNotFoundProcess(receiver, pointAbsolutePathString)
                }
            } else {
                return simpleErrorProcess(exists, receiver, pointAbsolutePathString)
            }
        }

        val props = runCatching { fs.props(pointAbsolutePathString).await() }
        if (props.isFailure) {
            return simpleErrorProcess(props, receiver, pointAbsolutePathString)
        }

        val info = fileInfo(pointAbsolutePathString, props.getOrThrow())
        return receiver.receive(State.OK, info)
    }

    private fun simpleNotFoundProcess(
        receiver: FileInfoReceiver, pointAbsolutePathString: String
    ): Boolean {
        val pathInfo = FilePathInfo(absolutePath, absolute(pointAbsolutePathString))
        receiver.receive(State.NOT_FOUND, pathInfo)
        return false
    }

    private fun simpleErrorProcess(
        result: Result<Any>, receiver: FileInfoReceiver, pointAbsolutePathString: String
    ): Boolean {
        log.error("读取文件错误", result.exceptionOrNull())
        val pathInfo = FilePathInfo(absolutePath, absolute(pointAbsolutePathString))
        receiver.receive(State.ERROR, pathInfo)
        return false
    }

    private fun fileInfo(pointAbsolutePathString: String, props: FileProps): FileInfo {
        val pointAbsolutePath = Path(pointAbsolutePathString).toAbsolutePath()
        return FileInfoImpl(absolutePath, pointAbsolutePath, props)
    }

    private fun absolute(path: String): Path {
        return Path(absolutePathString, path).toAbsolutePath()
    }

    fun interface FileInfoReceiver {

        fun receive(state: State, info: FileInfo): Boolean

    }

}
