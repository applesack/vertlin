package xyz.scootaloo.vertlin.boot.crontab

import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.Ordered
import xyz.scootaloo.vertlin.boot.ServiceLifeCycle
import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.core.currentTimeMillis
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ServiceManager
import xyz.scootaloo.vertlin.boot.resolver.ServiceReducer
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.util.LinkedList
import java.util.TreeMap
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午10:51
 */
object CrontabResolver : ServiceResolver(), ServiceReducer<CrontabManifest> {

    override fun acceptType(): KClass<*> {
        return Crontab::class
    }

    override fun solve(type: KClass<*>, manager: ServiceManager) {
        val context = solveContext(type)
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type)
        val crontab = instance as Crontab
        val manifest = CrontabManifest(context, crontab)
        manager.registerManifest(manifest)
    }

    override fun acceptSourceType(): KClass<CrontabManifest> {
        return CrontabManifest::class
    }

    override fun reduce(services: MutableList<ContextServiceManifest>, manager: ServiceManager) {
        val contexts = transfer(services).groupBy { it.context() }
        for ((context, crontab) in contexts) {
            val crontabMgr = CrontabManagerManifest(context)
            crontabMgr.initialize(crontab.map { it.crontab })

            manager.registerManifest(crontabMgr)
            manager.publishContextSingleton(crontabMgr, context, CrontabManager::class)
        }
    }

    private class CrontabManagerManifest(
        private val context: String,
        private val taskQue: TreeMap<String, CrontabWrapper> = TreeMap()
    ) : ContextServiceManifest, CrontabManager, ServiceLifeCycle, Helper {

        private val log = getLogger("$context-crontab")

        private val vertx by inject(Vertx::class)

        override fun name(): String {
            return "crontab-$context"
        }

        override fun context(): String {
            return context
        }

        override suspend fun register(vertx: Vertx) {
        }

        override fun publishCrontab(crontab: Crontab) {
            addCrontab(crontab)
        }

        override suspend fun initialize() {
            vertx.setPeriodic(CrontabDefault.defDelay) {
                run()
            }
        }

        fun initialize(crontabSet: Collection<Crontab>) {
            crontabSet.forEach { addCrontab(it) }
        }

        private fun run() {
            val currentTimeMills = currentTimeMillis()
            val invalidTaskIds = LinkedList<String>()
            for ((id, wrapper) in taskQue) {
                val crontab = wrapper.crontab
                val interval = currentTimeMills - wrapper.lastExecuteTime
                if (interval < crontab.delay) {
                    continue
                }

                wrapper.lastExecuteTime = currentTimeMills
                val result = runCatching {
                    crontab.run(currentTimeMills)
                    crontab.valid
                }

                if (result.isFailure) {
                    log.error(
                        "定时任务错误[$context]: 执行中报错, 任务名'${crontab.id}'",
                        result.exceptionOrNull()
                    )
                    continue
                }

                if (result.getOrNull() != true) {
                    invalidTaskIds.add(id)
                }
            }

            if (invalidTaskIds.isNotEmpty()) {
                invalidTaskIds.forEach {
                    log.info("定时任务[$context]: 移除失效的任务'$it'")
                    taskQue.remove(it)
                }
            }
        }

        private fun addCrontab(crontab: Crontab) {
            val wrapper = wrap(crontab)
            taskQue[wrapper.id] = wrapper
        }

        private fun wrap(crontab: Crontab): CrontabWrapper {
            val id = "${Ordered.suitable(crontab.order)}${crontab.id}"
            return CrontabWrapper(id, currentTimeMillis(), crontab)
        }

        override fun toString(): String {
            return "CrontabManagerManifest(context='$context', taskQue=$taskQue)"
        }

    }

    private class CrontabWrapper(
        val id: String,
        var lastExecuteTime: Long,
        val crontab: Crontab
    )

}
