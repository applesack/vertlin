package xyz.scootaloo.vertlin.boot.resolver

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午4:20
 */
interface ServiceManager {

    fun registerManifest(manifest: ContextServiceManifest)

    fun publishSingleton(ins: Any)

}
