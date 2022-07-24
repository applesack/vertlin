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

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午3:28
 */
internal object ServiceManagerImpl : ServiceManager, Helper {

    private val log = getLogger()

    private val manifests = HashMap<String, MutableList<ContextServiceManifest>>()
    private val singletons = HashMap<String, Any>()

    override fun registerManifest(manifest: ContextServiceManifest) {
        val type = TypeUtils.solveQualifiedName(manifest::class)
        CUtils.grouping(manifests, type, manifest) { LinkedList() }
    }

    override fun publishSingleton(ins: Any) {
        val typeQName = TypeUtils.solveQualifiedName(ins::class)
        if (typeQName in singletons) {
            val caller = Rearview.formatCaller(4)
            log.warn(
                "服务解析警告: 重复的单例注册, 由于类型'$typeQName'已存在, 所以当前实例被忽略; 调用点'$caller'"
            )
            return
        }

        singletons[typeQName] = ins
    }

    fun reduce(reducer: ServiceReducer<*>) {
        val acc = reducer.acceptSourceType()
        val typeQName = TypeUtils.solveQualifiedName(acc)
        val list = manifests.remove(typeQName) ?: return

        val reduced = reducer.reduce(list)
        val reducedTypeQName = TypeUtils.solveQualifiedName(reduced::class)
        CUtils.grouping(manifests, reducedTypeQName, reduced) { LinkedList() }
    }

    fun publishAllSingletons() {
        for (singleton in singletons.values) {
            Container.registerObject(singleton)
        }
    }

    fun displayManifests(): List<ContextServiceManifest> {
        return manifests.flatMap { it.value }
    }

    fun clearCache() {
        manifests.clear()
        singletons.clear()
    }

}
