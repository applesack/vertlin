package xyz.scootaloo.vertlin.dav.service

import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.file.copyOptionsOf
import io.vertx.kotlin.coroutines.await
import xyz.scootaloo.vertlin.dav.util.DualFileOperations

/**
 * @author flutterdash@qq.com
 * @since 2022/8/10 下午11:15
 */
object MoveService : DualFileOperations() {

    suspend fun move(ctx: RoutingContext) {
        handle(ctx)
    }

    private val copyOptional by lazy {
        copyOptionsOf(atomicMove = false, replaceExisting = true)
    }

    override fun actionName(): String {
        return "移动文件"
    }

    override suspend fun action(from: String, to: String) {
        fs.move(from, to, copyOptional).await()
    }

    override suspend fun actionRecursive(from: String, to: String) {
        fs.move(from, to, copyOptional).await()
    }

}
