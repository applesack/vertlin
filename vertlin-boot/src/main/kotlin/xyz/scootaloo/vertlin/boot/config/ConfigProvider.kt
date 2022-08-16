package xyz.scootaloo.vertlin.boot.config

import xyz.scootaloo.vertlin.boot.Service

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午8:27
 */
interface ConfigProvider : Service {

    fun register(editor: ConfigCheckerEditor)

}
