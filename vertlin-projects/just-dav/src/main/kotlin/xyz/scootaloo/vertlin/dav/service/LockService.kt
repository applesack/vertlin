package xyz.scootaloo.vertlin.dav.service

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import xyz.scootaloo.vertlin.boot.core.awaitParallelBlocking
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.ServerDefault
import xyz.scootaloo.vertlin.dav.constant.StatusCode
import xyz.scootaloo.vertlin.dav.domain.LockBlock
import xyz.scootaloo.vertlin.dav.lock.LockManager
import xyz.scootaloo.vertlin.dav.parse.header.DepthHeaderParser
import xyz.scootaloo.vertlin.dav.parse.header.IfHeaderParser
import xyz.scootaloo.vertlin.dav.parse.header.TimeoutHeaderParser
import xyz.scootaloo.vertlin.dav.parse.xml.LockRequestParser

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午3:19
 */
object LockService {

    private val lockManager by inject(LockManager::class)

    suspend fun lock(ctx: RoutingContext) {
        val lockInfo = parseRequestContent(ctx) ?: return ctx.fail(StatusCode.badRequest)
        val serialized = JsonObject.mapFrom(lockInfo).toString()
        if (lockInfo.condition != null) {
//            lockManager.refreshLock(serialized)
            ctx.fail(500)
        } else {
//            lockManager.lock(serialized)
            ctx.fail(500)
        }
    }

    private suspend fun parseRequestContent(ctx: RoutingContext): LockBlock? {
        return awaitParallelBlocking await@{
            val target = ctx.pathParam("*") ?: "/"

            val headers = ctx.request().headers()
            val ifCondition = IfHeaderParser.parseIfCondition(headers.get(HttpHeaders.IF))
            val depth = DepthHeaderParser.parseDepth(headers.get(HttpHeaders.DEPTH)) ?: ServerDefault.depth
            val timeout = TimeoutHeaderParser.parseTimeout(headers.get(HttpHeaders.TIMEOUT))
                ?: ServerDefault.timeout
            val body = LockRequestParser.parseLockBody(ctx.body().asString()) ?: return@await null

            LockBlock(target, ifCondition, depth, timeout, body)
        }
    }

}
