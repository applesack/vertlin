package xyz.scootaloo.vertlin.boot.resolver.impl

import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ServiceManager
import xyz.scootaloo.vertlin.boot.resolver.ServiceReducer
import xyz.scootaloo.vertlin.boot.util.CUtils
import xyz.scootaloo.vertlin.boot.util.Rearview
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午3:28
 */
internal object ServiceManagerImpl : ServiceManager, Helper {

    private val log = getLogger()

    private val manifests = HashMap<String, MutableList<ContextServiceManifest>>()
    private val singletons = HashMap<String, Pair<KClass<out Any>, Any>>()
    private val contextSingletons = HashMap<String, HashMap<String, Pair<KClass<out Any>, Any>>>()

    override fun registerManifest(manifest: ContextServiceManifest) {
        val type = TypeUtils.solveQualifiedName(manifest::class)
        CUtils.grouping(manifests, type, manifest) { LinkedList() }
    }

    override fun publishSingleton(ins: Any, type: KClass<out Any>) {
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

    fun reduce(reducer: ServiceReducer<*>) {
        val acc = reducer.acceptSourceType()
        val typeQName = TypeUtils.solveQualifiedName(acc)
        val list = manifests.remove(typeQName) ?: return

        reducer.reduce(list, this)
    }

    fun publishAllSingletons() {
        for ((type, ins) in singletons.values) {
            Container.registerSingleton(ins, type)
        }
        for ((context, mapper) in contextSingletons) {
            for ((_, pair) in mapper) {
                val (type, ins) = pair
                Container.registerContextSingleton(ins, context, type)
            }
        }
    }

    fun displayManifests(): List<ContextServiceManifest> {
        return manifests.flatMap { it.value }
    }

    fun clearCache() {
        manifests.clear()
        singletons.clear()
        contextSingletons.clear()
    }

}
