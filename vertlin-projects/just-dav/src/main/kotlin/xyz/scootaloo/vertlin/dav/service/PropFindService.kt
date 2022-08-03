package xyz.scootaloo.vertlin.dav.service

import io.vertx.core.file.FileSystem
import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xyz.scootaloo.vertlin.boot.core.awaitParallelBlocking
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.ServerDefault
import xyz.scootaloo.vertlin.dav.domain.PropFindBlock
import xyz.scootaloo.vertlin.dav.lock.LockManager
import xyz.scootaloo.vertlin.dav.parse.header.DepthHeaderParser

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午4:32
 */
object PropFindService {

    private val fs by inject(FileSystem::class)
    private val lockManager by inject(LockManager::class)

    suspend fun propFind(ctx: RoutingContext) {
        val block = parseRequestContent(ctx)
        val deniedSet = Json.decodeFromString<List<String>>(lockManager.detect())
    }

    private suspend fun parseRequestContent(ctx: RoutingContext): PropFindBlock {
        return awaitParallelBlocking {
            val headers = ctx.request().headers()
            val target = ctx.pathParam("*") ?: "/"
            val depth = DepthHeaderParser.parseDepth(headers.get(HttpHeaders.DEPTH)) ?: ServerDefault.depth
            PropFindBlock(target, depth)
        }
    }

    private fun buildResponse(block: PropFindBlock): String {
        return ""
    }

}
