package xyz.scootaloo.vertlin.dav.application

import xyz.scootaloo.vertlin.boot.config.ConfigCheckerEditor
import xyz.scootaloo.vertlin.boot.config.ConfigProvider

/**
 * @author flutterdash@qq.com
 * @since 2022/8/16 下午11:12
 */
class ConfigChecker : ConfigProvider {

    override fun register(editor: ConfigCheckerEditor) {
        editor.key("webdav.path", String::class)
        editor.key("webdav.cache", Int::class)
        editor.key("webdav.users", Map::class)
    }

}
