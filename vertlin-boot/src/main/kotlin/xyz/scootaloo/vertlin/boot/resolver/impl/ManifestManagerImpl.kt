package xyz.scootaloo.vertlin.boot.resolver.impl

import xyz.scootaloo.vertlin.boot.core.X
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
    private val singletons = HashMap<String, Pair<KClass<out Any>, Any>>()
    private val contextSingletons = HashMap<String, HashMap<String, Pair<KClass<out Any>, Any>>>()

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
        if (closed) {
            return
        }
        val typeQName = TypeUtils.solveQualifiedName(type)
        if (typeQName in singletons) {
            val caller = Rearview.formatCaller(4)
            log.warn(
                "服务解析警告: 重复的单例注册, 由于类型'$typeQName'已存在, 所以当前实例被忽略; 调用点'$caller'"
            )
            return
        }

        singletons[typeQName] = type to ins
    }

    override fun publishContextSingleton(ins: Any, context: String, type: KClass<out Any>) {
        if (closed) {
            return
        }
        val mapper = contextSingletons[context] ?: HashMap()
        contextSingletons[context] = mapper

        val typeQName = TypeUtils.solveQualifiedName(type)
        if (typeQName in mapper) {
            val caller = Rearview.formatCaller(4)
            log.warn(
                "服务解析警告: 重复的单例注册, 在上下文'$context'中, 类型'$typeQName'; 调用点:'$caller'"
            )
            return
        }

        mapper[typeQName] = type to ins
    }

    fun reduce(reducer: ManifestReducer) {
        reducer.reduce(this)
    }

    fun publishAllSingletons() {
        for ((type, ins) in singletons.values) {
            Container.registerSharedSingleton(ins, type)
        }
        for ((context, mapper) in contextSingletons) {
            for ((_, pair) in mapper) {
                val (type, ins) = pair
                Container.registerContextSingleton(ins, context, type)
            }
        }
        singletons.clear()
        contextSingletons.clear()
    }

    fun displayManifests(): List<ContextServiceManifest> {
        return manifests.flatMap { it.value }
    }

    fun clearCache() {
        closed = true
        manifests.clear()
        singletons.clear()
        contextSingletons.clear()
    }

}
