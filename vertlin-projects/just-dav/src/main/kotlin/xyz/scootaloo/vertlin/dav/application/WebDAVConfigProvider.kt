package xyz.scootaloo.vertlin.dav.application

import xyz.scootaloo.vertlin.boot.config.ConfigCheckerEditor
import xyz.scootaloo.vertlin.boot.config.ConfigProvider

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午5:35
 */
class WebDAVConfigProvider : ConfigProvider {

    override fun register(editor: ConfigCheckerEditor) {
        editor.key("webdav.path", String::class)
        editor.key("webdav.users", Map::class)
    }

}
