package xyz.scootaloo.vertlin.dav.domain

import io.vertx.ext.web.RoutingContext
import kotlinx.serialization.Serializable
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.constant.HttpHeaders
import xyz.scootaloo.vertlin.dav.constant.ServerDefault
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.parse.header.DepthHeaderParser
import xyz.scootaloo.vertlin.dav.parse.header.IfHeaderParser
import xyz.scootaloo.vertlin.dav.util.PathUtils
import xyz.scootaloo.vertlin.web.HttpServerConfig

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

        private val server by inject(HttpServerConfig::class)

        fun of(ctx: RoutingContext, defTargetPath: String = "/"): AccessBlock {
            val headers = ctx.request().headers()

            // 一个路径解码的bug, 客户端请求一个路径, 而这个路径使用uriComponentEncode加密过, 这段加密的文本中包含符号'+'
            // 符号'+'对应的字符是空格' ', 但是'+'在解码的时候并没有被还原为空格
            // 导致实际路径和通过[ctx.pathParam]获取到的值不一致

            // 路径加密使用算法见 [PathUtils]
            // 处理方案: 由于这个项目提取路径参数主要是取后缀, 所以使用手动获取uri并解密代替 [ctx.pathParam]

            val target = FileInfo.normalize(decodeUriComponent(ctx, defTargetPath))

            val condition = IfHeaderParser.parseIfCondition(headers.get(HttpHeaders.IF))
            val depth = DepthHeaderParser.parseDepth(headers.get(HttpHeaders.DEPTH)) ?: ServerDefault.depth

            return AccessBlock(target, condition, depth)
        }

        private fun decodeUriComponent(ctx: RoutingContext, def: String): String {
            val rawUri = ctx.request().uri()
            if (rawUri.isEmpty()) return def
            val uriComponent = rawUri.substring(server.prefix.length)
            if (uriComponent == def)
                return def
            return PathUtils.decodeUriComponent(uriComponent)
        }

    }
}
