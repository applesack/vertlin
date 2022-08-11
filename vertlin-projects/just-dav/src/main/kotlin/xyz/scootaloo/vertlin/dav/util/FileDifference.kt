package xyz.scootaloo.vertlin.dav.util

import io.vertx.core.file.FileProps
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.dav.file.FileInfo
import java.util.LinkedList
import kotlin.io.path.absolutePathString

/**
 * @author flutterdash@qq.com
 * @since 2022/8/8 下午11:26
 */
object FileDifference : FileOperations() {

    /**
     * 构建源路径和目的路径的交集
     *
     * 前置条件:
     * 1. 源路径必须经过检查(必须是存在的)
     * 2. 源路径必须和目的路径必须是文件夹
     */
    suspend fun intersect(param: DiffParam, skipSet: Collection<String> = emptyList()): Node {
        val sourceAbsolutePath = absolute(param.source).absolutePathString()
        val destAbsolutePath = absolute(param.destination).absolutePathString()

        val root = Node(false, Status.PLACEHOLDER)
        skipSet.forEach { insert(root, param.source, it, Status.SKIP, true) }

        param.sourceDeniedSet.forEach { insert(root, param.source, it, Status.LOCKED, true) }
        param.destinationDeniedSet.forEach {
            insert(root, param.destination, it, Status.LOCKED, false)
        }

        if (!fs.exists(destAbsolutePath).await()) {
            return root
        }

        val destProps = fs.props(destAbsolutePath).await()
        if (!destProps.isDirectory) {
            root.status = Status.DUPLICATE
            root.destType = FileType.FILE
            return root
        }

        intersect(root, sourceAbsolutePath, destAbsolutePath)

        return root
    }

    private suspend fun intersect(
        root: Node, sourceAbsolutePath: String, destAbsolutePath: String
    ) {
        val sourceDirContents = fs.readDir(sourceAbsolutePath).await()
        val destDirContents = fs.readDir(destAbsolutePath).await()

        // 组织成键值对: 文件名 => 完整路径
        val sourceDirMembers = sourceDirContents.associateBy { PathUtils.filename(it) }
        val destDirMembers = destDirContents.associateBy { PathUtils.filename(it) }

        val candidates = LinkedList<Triple<Node, String, String>>()

        // 遍历所有成员, 选取重复项目加入root
        for ((memberName, sourcePath) in sourceDirMembers) {
            if (memberName in destDirMembers) {
                val sourceProp = fs.props(sourcePath).await()
                val destProp = fs.props(destDirMembers[memberName]).await()

                val diff = Node(false, Status.DUPLICATE)
                diff.sourceType = fileTypeOf(sourceProp)
                diff.destType = fileTypeOf(destProp)

                root[memberName] = diff

                if (diff.sourceType == FileType.DIR && diff.destType == FileType.DIR) {
                    diff.status = Status.PLACEHOLDER

                    val triple = Triple(diff, sourcePath, sourceDirMembers[memberName]!!)
                    candidates.add(triple)
                }
            }
        }

        // 候选项集合是重复项目中文件夹类型,
        for ((node, s, d) in candidates) {
            intersect(node, s, d)
        }
    }

    private fun insert(
        receiver: Node, prefix: String, subPath: String,
        status: Status, isSource: Boolean
    ) {
        val path = subPath.substring(FileInfo.normalize(prefix).length + 1)
        val items = path.split('/')
        var current = receiver
        for (item in items) {
            if (item in current) {
                current = current[item]!!
            } else {
                val tmpNode = Node(isSource, Status.PLACEHOLDER)
                current[item] = tmpNode
                current = tmpNode
            }
        }

        current.isSource = isSource
        if (status != Status.LOCKED) {
            current.status = status
        }
    }

    private fun fileTypeOf(props: FileProps): FileType {
        if (props.isDirectory) return FileType.DIR
        return FileType.FILE
    }

    class DiffParam(
        val source: String,
        val destination: String,
        val sourceDeniedSet: List<String>,
        val destinationDeniedSet: List<String>,
        val isInfinite: Boolean,
        val overwrite: Boolean
    )

    enum class Status {

        /* 无法处理, 记录即可 */
        LOCKED,

        /* 跳过, 但需要记录 */
        SKIP,

        /* 重复项, 原地处理 */
        DUPLICATE,

        /* 占位符: 需要递归处理 */
        PLACEHOLDER

    }

    enum class FileType {
        FILE, DIR
    }

    class Node(
        var isSource: Boolean,
        var status: Status,
        var sourceType: FileType = FileType.FILE,
        var destType: FileType = FileType.FILE,
        private val subNodes: HashMap<String, Node> = HashMap()
    ) : MutableMap<String, Node> by subNodes

}
