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
    val port: Int,
    val prefix: String,
    val enableLog: Boolean
) : Config


class HttpServerConfigProvider : ConfigProvider {

    private val log = X.getLogger(this::class)

    private val port = "http.port"
    private val prefix = "http.prefix"
    private val enableLog = "http.enableLog"

    override fun register(manager: ConfigManager) {
        manager.registerChecker(port, ::checkPort)
        manager.registerChecker(prefix, ::checkPrefix)
        manager.registerChecker(enableLog, ::checkEnableLog)

        manager.registerDefault(port, 8080)
        manager.registerDefault(prefix, "")
        manager.registerDefault(enableLog, true)
    }

    private fun checkPort(value: Any): Boolean {
        if (value !is Number) {
            logTypeError(port, Int::class, log)
            return false
        }
        val num = value.toInt()
        if (num < 1 || num > 65535) {
            log.warn("配置项'${port}'取值为1~65535")
            return false
        }
        return true
    }

    private fun checkPrefix(value: Any): Boolean {
        if (value !is String) {
            logTypeError(prefix, String::class, log)
            return false
        }
        return true
    }

    private fun checkEnableLog(value: Any): Boolean {
        if (value !is Boolean) {
            logTypeError(enableLog, Boolean::class, log)
            return false
        }
        return true
    }

}
