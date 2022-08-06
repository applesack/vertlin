package xyz.scootaloo.vertlin.boot.config

import io.vertx.core.impl.logging.Logger
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午8:27
 */
interface ConfigProvider {

    fun register(manager: ConfigManager)

    fun logTypeError(key: String, accept: KClass<*>, log: Logger) {
        log.error("配置项错误: 格式错误, 在$key, 类型应该为$accept")
    }

}
