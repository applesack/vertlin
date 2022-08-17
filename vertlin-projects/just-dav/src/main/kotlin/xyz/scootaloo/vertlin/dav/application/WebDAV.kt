package xyz.scootaloo.vertlin.dav.application

import xyz.scootaloo.vertlin.boot.config.Config
import xyz.scootaloo.vertlin.boot.config.Prefix

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午8:54
 */
@Prefix("webdav")
class WebDAV(
    val path: String,
    val cache: Int,
    val users: Map<String, String>
) : Config
