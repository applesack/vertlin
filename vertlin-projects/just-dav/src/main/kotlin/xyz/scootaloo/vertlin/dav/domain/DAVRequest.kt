package xyz.scootaloo.vertlin.dav.domain

import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.Serializable
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.ServerDefault
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.parse.header.DepthHeaderParser
import xyz.scootaloo.vertlin.dav.parse.header.IfHeaderParser

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午4:55
 */
@Serializable
data class LockBlock(
    val target: String,
    val condition: IfHeader?,
    val depth: DepthHeader,
    val timeout: TimeoutHeader,
    val body: LockBody
)


@Serializable
data class AccessBlock(
    val target: String,
    val condition: IfHeader?,
    val depth: DepthHeader
) {
    companion object {
        fun of(ctx: RoutingContext, defTargetPath: String = "/"): AccessBlock {
            val headers = ctx.request().headers()
            val target = FileInfo.normalize(ctx.pathParam("*") ?: defTargetPath)
            val condition = IfHeaderParser.parseIfCondition(headers.get(HttpHeaders.IF))
            val depth = DepthHeaderParser.parseDepth(headers.get(HttpHeaders.DEPTH)) ?: ServerDefault.depth
            return AccessBlock(target, condition, depth)
        }
    }
}
