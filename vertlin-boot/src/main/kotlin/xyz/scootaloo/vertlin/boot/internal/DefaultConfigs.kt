package xyz.scootaloo.vertlin.boot.internal

import xyz.scootaloo.vertlin.boot.config.ConfigManager
import xyz.scootaloo.vertlin.boot.config.ConfigProvider

/**
 * @author flutterdash@qq.com
 * @since 2022/7/28 下午9:33
 */
class DefaultConfigs : ConfigProvider {

    override fun register(manager: ConfigManager) {
        manager.registerChecker("vertx.workerPoolSize") { it is Int }
        manager.registerChecker("profiles.prefix") { it is String }
        manager.registerChecker("profiles.active") { it is String }
    }

}
