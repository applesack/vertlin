package xyz.scootaloo.vertlin.hello

import xyz.scootaloo.vertlin.boot.config.Config
import xyz.scootaloo.vertlin.boot.config.Prefix

/**
 * @author flutterdash@qq.com
 * @since 2022/8/16 下午1:52
 */
@Prefix("test")
class MyConfig(
    val name: String
) : Config
