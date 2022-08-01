package xyz.scootaloo.vertlin.boot.resolver

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 上午9:46
 */
interface ManifestManager : ResourcesPublisher {

    fun <T : ContextServiceManifest> extractManifests(type: KClass<T>): List<T>

}
