package xyz.scootaloo.vertlin.dav.application

import xyz.scootaloo.vertlin.boot.config.ConfigManager
import xyz.scootaloo.vertlin.boot.config.ConfigProvider
import xyz.scootaloo.vertlin.boot.core.X

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午5:35
 */
class WebDAVConfigProvider : ConfigProvider {

    private val log = X.getLogger(this::class)

    override fun register(manager: ConfigManager) {
        manager.registerDefault("webdav.path", "home")
        manager.registerDefault("webdav.users", HashMap<String, String>())

        manager.registerChecker("webdav.path") { it is String }
        manager.registerChecker("webdav.users", ::userConfigCheck)
    }

    private fun userConfigCheck(any: Any): Boolean {
        if (any !is Map<*, *>) {
            log.error("配置项格式错误: webdav.users属性必须为键值对")
            return false
        }

        for ((key, value) in any) {
            if (value != null && value !is String) {
                log.error("配置项格式错误: 在webdav.users, 用户名为$key, 密码必须是字符串(使用双引号)")
                return false
            }
            if (value is String && value.isBlank()) {
                log.error("配置项格式错误: 在webdav.users, 用户名为$key, 密码不能为空")
                return false
            }
        }

        return true
    }

}
