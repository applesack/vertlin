package xyz.scootaloo.vertlin.web

import xyz.scootaloo.vertlin.boot.config.Config
import xyz.scootaloo.vertlin.boot.config.ConfigManager
import xyz.scootaloo.vertlin.boot.config.ConfigProvider
import xyz.scootaloo.vertlin.boot.config.Prefix
import xyz.scootaloo.vertlin.boot.core.X

/**
 * @author flutterdash@qq.com
 * @since 2022/7/30 下午5:56
 */
@Prefix("http")
class HttpServerConfig(
    val port: Int
) : Config


class HttpServerConfigProvider : ConfigProvider {

    private val log = X.getLogger(this::class)

    private val port = "http.port"

    override fun register(manager: ConfigManager) {
        manager.registerChecker(port) { checkPort(it) }
        manager.registerDefault(port, 8080)
    }

    private fun checkPort(value: Any): Boolean {
        if (value !is Int) {
            log.warn("配置项'${port}'必须是数字")
            return false
        }
        val number = value as Int
        if (number !in 1..65536) {
            log.warn("配置项'${port}'取值为1~65535")
            return false
        }
        return true
    }

}
