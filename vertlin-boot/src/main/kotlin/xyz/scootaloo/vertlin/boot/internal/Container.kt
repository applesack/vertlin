package xyz.scootaloo.vertlin.boot.internal

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.file.FileSystem
import xyz.scootaloo.vertlin.boot.LazyInit
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * 资源管理
 *
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午11:29
 */
internal object Container {

    private val stateLock = ReentrantReadWriteLock()
    private var state = StartupState.CLOSED

    private val resources = HashMap<String, KClass<*>>()
    private val singletonMapper = HashMap<String, Any>()

    private val sharedSingletonLock = ReentrantReadWriteLock()
    private val sharedSingletonMapper = ConcurrentHashMap<String, Any>()

    private val contextRegisterLock = ReentrantReadWriteLock()
    private val contextRegisterMapper = ConcurrentHashMap<String, String>()

    private val contextSingletonsLock = ReentrantReadWriteLock()
    private val contextSingletonMapper = ConcurrentHashMap<String, HashMap<String, Any>>()

    fun getObject(type: KClass<*>): Any? {
        val typeQName = TypeUtils.solveQualifiedName(type)
        return sharedSingletonLock.read {
            sharedSingletonMapper[typeQName]
        }
    }

    fun getContextObject(type: KClass<*>): Any? {
        val threadName = realThreadName()
        val context = contextRegisterLock.read {
            contextRegisterMapper[threadName]
        } ?: return null

        return getContextObject(context, type)
    }

    fun getContextObject(context: String, type: KClass<*>): Any? {
        val typeQName = TypeUtils.solveQualifiedName(type)
        return contextSingletonsLock.read ret@{
            val mapper = contextSingletonMapper[context] ?: return@ret null
            return mapper[typeQName]
        }
    }

    fun loadResources(resources: Collection<KClass<*>>) {
        for (res in resources) {
            val typeQName = TypeUtils.solveQualifiedName(res)
            if (typeQName !in this.resources) {
                this.resources[typeQName] = res
            }
        }
    }

    fun registerResource(instance: Any, facade: KClass<*> = instance::class) {
        val typeQName = TypeUtils.solveQualifiedName(facade)
        if (typeQName !in resources) {
            resources[typeQName] = facade
        }
        singletonMapper[typeQName] = instance
    }

    fun <T : Any> instancesOf(superType: KClass<T>): List<Pair<KClass<*>, T?>> {
        val results = LinkedList<Pair<KClass<*>, T?>>()
        for (type in resources.values) {
            if (type.isSubclassOf(superType)) {
                @Suppress("UNCHECKED_CAST")
                val ins: T? = instantiate(type) as T?
                results.add(type to ins)
            }
        }
        return results
    }

    fun start() {
        modifyStartupState(StartupState.STARTING)
    }

    fun failure() {
        modifyStartupState(StartupState.FAILURE)
    }

    fun finish() {
        modifyStartupState(StartupState.FINISHED)
    }

    fun registerSharedSingleton(obj: Any, type: KClass<*> = obj::class) {
        val typeQName = TypeUtils.solveQualifiedName(type)
        sharedSingletonLock.write {
            sharedSingletonMapper[typeQName] = obj
        }
    }

    fun registerContextSingleton(
        obj: Any, context: String, type: KClass<out Any> = obj::class
    ) {
        val typeQName = TypeUtils.solveQualifiedName(type)
        contextSingletonsLock.write {
            val mapper = contextSingletonMapper[context] ?: HashMap()
            contextSingletonMapper[context] = mapper
            mapper[typeQName] = obj
        }
    }

    fun registerContextMapper(contextName: String) {
        val threadName = realThreadName()
        contextRegisterLock.write {
            contextRegisterMapper[threadName] = contextName
        }
    }

    private fun instantiate(type: KClass<*>): Any? {
        if (type.isSubclassOf(LazyInit::class))
            return null

        checkState(StartupState.STARTING)
        val typeQName = TypeUtils.solveQualifiedName(type)
        if (typeQName in singletonMapper) {
            return singletonMapper[typeQName]!!
        }
        val instance = TypeUtils.createInstanceByNonArgsConstructor(type)
        singletonMapper[typeQName] = instance
        return instance
    }

    private fun realThreadName(): String {
        val threadName = Thread.currentThread().name
        val idx = threadName.lastIndexOf(' ')
        if (idx > 0) {
            return threadName.substring(0, idx)
        }
        return threadName
    }

    /**
     * 指定当前系统状态
     *
     * [not]是否反选
     *
     * [targetState] = [StartupState.STARTING], [not] = false
     * 当前系统状态必须为[StartupState.STARTING]
     *
     * [targetState] = [StartupState.STARTING], [not] = true
     * 当前系统状态不能是[StartupState.STARTING]
     */
    private fun checkState(targetState: StartupState, not: Boolean = false) {
        stateLock.read {
            val expr = if (not) targetState != state else targetState == state
            if (!expr) {
                val msg = buildString {
                    append("状态异常: ")
                    append("当前操作需要系统状态")
                    if (not) {
                        append("不为")
                    } else {
                        append("为")
                    }
                    append(targetState)
                    append("; 当前系统状态为")
                    append(state)
                }

                throw IllegalStateException(msg)
            }
        }
    }

    private fun modifyStartupState(new: StartupState) {
        stateLock.write {
            if (state != new) {
                state = new
            }
        }
    }

    enum class StartupState {
        CLOSED, STARTING, FAILURE, FINISHED
    }

}
