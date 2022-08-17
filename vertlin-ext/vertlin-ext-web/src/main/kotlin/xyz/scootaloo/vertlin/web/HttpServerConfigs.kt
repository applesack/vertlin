package xyz.scootaloo.vertlin.web

import xyz.scootaloo.vertlin.boot.config.Config
import xyz.scootaloo.vertlin.boot.config.ConfigCheckerEditor
import xyz.scootaloo.vertlin.boot.config.ConfigProvider
import xyz.scootaloo.vertlin.boot.config.Prefix

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


@Prefix("http.ssl")
class HttpSslConfig(
    val enable: Boolean,
    val path: String,
    val password: String
) : Config


class HttpServerConfigProvider : ConfigProvider {

    override fun register(editor: ConfigCheckerEditor) {
        editor.key("http.port", Int::class) {
            rangeTips("端口号取值为1~65535")
            range { it in 1..65536 }
        }

        editor.key("http.prefix", String::class)
        editor.key("http.bodyLimit", Long::class)
        editor.key("http.enableLog", Boolean::class)

        editor.key("http.ssl.enable", Boolean::class)
        editor.key("http.ssl.path", String::class)
        editor.key("http.ssl.password", String::class)
    }

}
