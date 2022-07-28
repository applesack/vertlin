package xyz.scootaloo.vertlin.boot.resolver

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午4:20
 */
interface ServiceManager {

    fun registerManifest(manifest: ContextServiceManifest)

    /**
     * 发布一个单例, 可以通过[type]来设定实例的真实类型
     */
    fun publishSharedSingleton(ins: Any, type: KClass<out Any> = ins::class)

    fun publishContextSingleton(ins: Any, context: String, type: KClass<out Any>)

}
