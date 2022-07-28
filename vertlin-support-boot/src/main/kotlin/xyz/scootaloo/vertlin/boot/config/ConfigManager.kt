package xyz.scootaloo.vertlin.boot.config

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午11:21
 */
interface ConfigManager {

    fun registerDefault(key: String, value: Any)

    fun registerChecker(key: String, check: (Any) -> Boolean)

    fun registerRequired(key: String, msg: String)

}
