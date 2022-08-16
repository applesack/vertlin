package xyz.scootaloo.vertlin.boot.internal

import xyz.scootaloo.vertlin.boot.config.ConfigCheckerEditor
import xyz.scootaloo.vertlin.boot.config.ConfigProvider

/**
 * @author flutterdash@qq.com
 * @since 2022/7/28 下午9:33
 */
class DefaultConfigs : ConfigProvider {

    override fun register(editor: ConfigCheckerEditor) {
        editor.key("vertx.workerPoolSize", Int::class) {
            rangeTips("取值为0~50")
            range { it in 0..50 }
        }

        editor.key("profiles.prefix", String::class)
        editor.key("profiles.active", String::class)
    }

}
