package xyz.scootaloo.vertlin.boot.config

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午8:27
 */
interface ConfigProvider {

    fun register(manager: ConfigManager)

}
