package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.file.copyOptionsOf
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.dav.util.DualFileOperations

/**
 * @author flutterdash@qq.com
 * @since 2022/8/8 下午3:38
 */
object CopyService : DualFileOperations() {

    /**
     * webdav copy
     *
     * 测试项目:
     * 1. 将源路径(文件)复制到目的路径(目录)
     * 2. 将源路径(目录)复制到目的路径(目录), 递归或者非递归
     * 3. 将源路径(目录)复制到源路径的子集
     * 4. 在源路径或目的路径存在锁的情况下复制
     */
    suspend fun copy(ctx: RoutingContext) {
        handle(ctx)
    }

    private val copyOptional by lazy { copyOptionsOf(atomicMove = false, replaceExisting = true) }

    override fun actionName(): String {
        return "复制文件"
    }

    override suspend fun action(from: String, to: String) {
        fs.copy(from, to, copyOptional).await()
    }

    override suspend fun actionRecursive(from: String, to: String) {
        fs.copyRecursive(from, to, true).await()
    }

}
