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
    val enableLog: Boolean,
    val bodyLimit: Long
) : Config


class HttpServerConfigProvider : ConfigProvider {

    private val log = X.getLogger(this::class)

    private val port = "http.port"
    private val prefix = "http.prefix"
    private val enableLog = "http.enableLog"
    private val bodyLimit = "http.bodyLimit"
    private val enableSsl = "http.ssl.enable"
    private val sslPath = "http.ssl.path"

    override fun register(manager: ConfigManager) {
        manager.registerChecker(port, ::checkPort)
        manager.registerChecker(prefix, ::checkPrefix)
        manager.registerChecker(enableLog, ::checkEnableLog)
        manager.registerChecker(bodyLimit, ::bodyLimitChecker)
        manager.registerChecker(enableSsl, ::enableSslChecker)

        manager.registerDefault(port, 8080)
        manager.registerDefault(prefix, "")
        manager.registerDefault(enableLog, true)
        manager.registerDefault(bodyLimit, 83886080L) // 50M
        manager.registerDefault(enableSsl, false)
    }

    private fun checkPort(value: Any): Boolean {
        if (value !is Number) {
            logTypeError(port, Int::class, log)
            return false
        }
        val num = value.toInt()
        if (num < 1 || num > 65535) {
            logRangeError(port, "1~65535", log)
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

    private fun bodyLimitChecker(value: Any): Boolean {
        if (value !is Number) {
            logTypeError(bodyLimit, Long::class, log)
            return false
        }
        return true
    }

    private fun enableSslChecker(value: Any): Boolean {
        if (value !is String) {
            logTypeError(enableSsl, String::class, log)
            return false
        }
        return true
    }

}
