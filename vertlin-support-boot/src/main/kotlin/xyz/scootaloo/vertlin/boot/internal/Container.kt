package xyz.scootaloo.vertlin.boot.internal

import io.vertx.core.Vertx
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

    private val resourcesLock = ReentrantReadWriteLock()
    private val coroutineRes = ConcurrentHashMap<String, CoroutineResource>()

    private val contextLock = ReentrantReadWriteLock()
    private val contextMapper = ConcurrentHashMap<String, String>()

    fun getObject(type: KClass<*>): Any? {
        checkState()
        val typeQName = TypeUtils.solveQualifiedName(type)
        return singletonLock.read {
            singletons[typeQName]
        }
    }

    fun getCoroutineRes(context: String): CoroutineResource {
        checkState()
        return resourcesLock.read {
            coroutineRes[context]!!
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
        registerObject(vertx)
        registerObject(vertx.eventBus())
        registerObject(vertx.fileSystem())
    }

    internal fun registerObject(obj: Any) {
        val typeQName = TypeUtils.solveQualifiedName(obj::class)
        singletonLock.write {
            singletons[typeQName] = obj
        }
    }

    internal fun registerCoroutineEntrance(context: String, res: CoroutineResource) {
        resourcesLock.write {
            coroutineRes[context] = res
        }
    }

    internal fun registerContextMapper(threadName: String, contextName: String) {
        contextLock.write {
            contextMapper[threadName] = contextName
        }
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
