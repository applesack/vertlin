package xyz.scootaloo.vertlin.boot.resolver.impl

import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ManifestManager
import xyz.scootaloo.vertlin.boot.resolver.ManifestReducer
import xyz.scootaloo.vertlin.boot.util.CUtils
import xyz.scootaloo.vertlin.boot.util.Rearview
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.util.*
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午3:28
 */
internal object ManifestManagerImpl : ManifestManager {

    private val log = X.getLogger(this::class)
    private var closed = false

    private val manifests = HashMap<String, MutableList<ContextServiceManifest>>()

    override fun <T : ContextServiceManifest> extractManifests(type: KClass<T>): List<T> {
        val typeQName = TypeUtils.solveQualifiedName(type)
        @Suppress("UNCHECKED_CAST")
        return (manifests.remove(typeQName) ?: emptyList<T>()) as List<T>
    }

    override fun registerManifest(manifest: ContextServiceManifest) {
        if (closed) {
            return
        }
        val type = TypeUtils.solveQualifiedName(manifest::class)
        CUtils.grouping(manifests, type, manifest) { LinkedList() }
    }

    override fun publishSharedSingleton(ins: Any, type: KClass<out Any>) {
        isClosed() ?: return
        if (Container.getObject(type) != null) {
            val typeQName = TypeUtils.solveQualifiedName(type)
            val caller = Rearview.formatCaller(4)
            log.warn(
                "服务解析警告: 重复的单例注册, 由于类型'$typeQName'已存在, 所以当前实例被忽略; 调用点'$caller'"
            )
            return
        }

        Container.registerSharedSingleton(ins, type)
    }

    override fun publishContextSingleton(ins: Any, context: String, type: KClass<out Any>) {
        isClosed() ?: return
        if (Container.getContextObject(context, type) != null) {
            val typeQName = TypeUtils.solveQualifiedName(type)
            val caller = Rearview.formatCaller(4)
            log.warn(
                "服务解析警告: 重复的单例注册, 在上下文'$context'中, 类型'$typeQName'; 调用点:'$caller'"
            )
            return
        }

        Container.registerContextSingleton(ins, context, type)
    }

    fun reduce(reducer: ManifestReducer) {
        reducer.reduce(this)
    }

    fun displayManifests(): List<ContextServiceManifest> {
        return manifests.flatMap { it.value }
    }

    fun clearCache() {
        closed = true
        manifests.clear()
    }

    private fun isClosed(): Any? {
        if (closed) return null
        return Constant.PRESET
    }

}
