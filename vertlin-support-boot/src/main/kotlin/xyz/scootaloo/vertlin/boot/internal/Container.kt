package xyz.scootaloo.vertlin.boot.internal

import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.file.FileSystem
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass

/**
 * 资源管理
 *
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午11:29
 */
internal object Container {

    private val stateLock = ReentrantReadWriteLock()
    private var state = StartupState.CLOSED

    @Volatile private lateinit var vertx: Vertx

    private val singletonLock = ReentrantReadWriteLock()
    private val singletons = ConcurrentHashMap<String, Any>()

    private val contextLock = ReentrantReadWriteLock()
    private val contextMapper = ConcurrentHashMap<String, String>()

    private val contextSingletonsLock = ReentrantReadWriteLock()
    private val contextSingletons = ConcurrentHashMap<String, HashMap<String, Any>>()

    fun getObject(type: KClass<*>): Any? {
        checkState()
        val typeQName = TypeUtils.solveQualifiedName(type)
        return singletonLock.read {
            singletons[typeQName]
        }
    }

    fun getContextObject(type: KClass<*>): Any? {
        checkState()
        val threadName = realThreadName()
        val contextName = contextLock.read {
            contextMapper[threadName]
        } ?: return null

        val typeQName = TypeUtils.solveQualifiedName(type)
        return contextSingletonsLock.read ret@{
            val mapper = contextSingletons[contextName] ?: return@ret null
            return mapper[typeQName]
        }
    }

    internal fun start() {
        modifyStartupState(StartupState.STARTING)
    }

    internal fun failure() {
        modifyStartupState(StartupState.FAILURE)
    }

    internal fun finish() {
        modifyStartupState(StartupState.FINISHED)
    }

    internal fun registerVertx(vertx: Vertx) {
        this.vertx = vertx
        registerSingleton(vertx, Vertx::class)
        registerSingleton(vertx.eventBus(), EventBus::class)
        registerSingleton(vertx.fileSystem(), FileSystem::class)
    }

    internal fun registerSingleton(obj: Any, type: KClass<*> = obj::class) {
        val typeQName = TypeUtils.solveQualifiedName(type)
        singletonLock.write {
            singletons[typeQName] = obj
        }
    }

    internal fun registerContextSingleton(
        obj: Any, context: String, type: KClass<out Any> = obj::class
    ) {
        val typeQName = TypeUtils.solveQualifiedName(type)
        contextSingletonsLock.write {
            val mapper = contextSingletons[context] ?: HashMap()
            contextSingletons[context] = mapper
            mapper[typeQName] = obj
        }
    }

    internal fun registerContextMapper(contextName: String) {
        val threadName = realThreadName()
        contextLock.write {
            contextMapper[threadName] = contextName
        }
    }

    private fun realThreadName(): String {
        val threadName = Thread.currentThread().name
        val idx = threadName.indexOf(' ')
        if (idx > 0) {
            return threadName.substring(0, idx)
        }
        return threadName
    }

    private fun checkState(targetState: StartupState = StartupState.FINISHED) {
        stateLock.read {
            if (targetState != state) {
                throw IllegalStateException(
                    "状态异常: 当前操作需要系统状态为'$targetState'; 当前系统状态'$state'"
                )
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
