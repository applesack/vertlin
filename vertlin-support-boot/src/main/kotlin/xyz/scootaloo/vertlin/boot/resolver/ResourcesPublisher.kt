package xyz.scootaloo.vertlin.boot.resolver

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午4:20
 */
interface ResourcesPublisher {

    fun registerManifest(manifest: ContextServiceManifest)

    /**
     * 发布一个共享单例, 可以通过[type]来设定实例的真实类型
     */
    fun publishSharedSingleton(ins: Any, type: KClass<out Any> = ins::class)

    /**
     * 发布一个上下文单例, 这个单例资源只有在对应的上下文中可以被访问
     */
    fun publishContextSingleton(ins: Any, context: String, type: KClass<out Any>)

}
