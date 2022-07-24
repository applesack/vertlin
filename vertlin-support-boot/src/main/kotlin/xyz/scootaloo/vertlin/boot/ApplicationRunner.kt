package xyz.scootaloo.vertlin.boot

import io.vertx.core.Future
import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApi
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApiResolver
import xyz.scootaloo.vertlin.boot.exception.InheritanceRelationshipException
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ServiceReducer
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.resolver.impl.ServiceManagerImpl
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import xyz.scootaloo.vertlin.boot.verticle.MainVerticle
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午5:10
 */
object ApplicationRunner : Helper {

    private val log = getLogger()

    private val manager = ServiceManagerImpl

    private val resolvers = HashMap<String, ServiceResolver>()
    private val manifests = LinkedList<ContextServiceManifest>()

    fun run(manifest: BootManifest, args: Array<String>): Future<Unit> {
        if (!readCommandLine(args)) {
            return Unit.wrapInFut()
        }

        return try {
            prepareStarting()
            loadResolvers(manifest.resolvers())
            loadServices(manifest.services())
            startServers().transform { done ->
                if (done.succeeded()) {
                    finish().wrapInFut()
                } else {
                    failure(done.cause()).wrapInFut()
                }
            }
        } catch (error: Throwable) {
            failure(error).wrapInFut()
        }
    }

    private fun readCommandLine(args: Array<String>): Boolean {
//        TODO()
        return true
    }

    private fun prepareStarting() {
        Container.start()
//        TODO("计时, 标记状态")
        // todo 在此读取配置文件
    }

    private fun loadResolvers(resolvers: Collection<ServiceResolver>) {
        for (resolver in resolvers) {
            val accept = resolver.acceptType()
            checkTypeOfResolverAccepts(accept)
            val typeQualified = TypeUtils.solveQualifiedName(accept)
            this.resolvers[typeQualified] = resolver
        }

        if (TypeUtils.solveQualifiedName(EventbusApi::class) !in this.resolvers) {
            this.resolvers[TypeUtils.solveQualifiedName(EventbusApi::class)] = EventbusApiResolver
        }
    }

    private fun checkTypeOfResolverAccepts(type: KClass<*>) {
        if (!type.isSubclassOf(Service::class)) {
            throw InheritanceRelationshipException(type, Service::class)
        }
    }

    private fun loadServices(services: Collection<KClass<out Service>>) {
        val reducers = resolvers.values.filterIsInstance<ServiceReducer<*>>()

        for (service in services) {
            var solved = false
            for (superType in service.supertypes) {
                val superTypeName = superType.toString()
                if (superTypeName in resolvers) {
                    val resolver = resolvers[superTypeName]!!
                    resolver.solve(service, manager)

                    solved = true
                    break
                }
            }
            if (!solved) {
                log.warn(
                    """\n
                    服务启动警告: 未在配置中找到能够处理类型为'$service'的处理器,
                    所以该类型为无效服务, 不会被载入到系统; 如果要解决这个异常,
                    请考虑实现xyz.scootaloo.vertlin.boot.resolver.ServiceResolver,
                    并将实现注入到启动配置中
                    """.trimIndent()
                )
            }
        }

        for (reducer in reducers) {
            manager.reduce(reducer)
        }

        manager.publishAllSingletons()
        manifests.addAll(manager.displayManifests())
    }

    private fun startServers(): Future<Vertx> {
        val contexts = manifests.groupBy { it.context() }.toMutableMap()
        val sysServices = contexts.remove(Constant.SYSTEM) ?: emptyList()
        sortServicesByPriority(contexts)

        val vertx = createVertxInstance()
        val main = MainVerticle(sysServices, contexts)
        return vertx.deployVerticle(main).trans { vertx }
    }

    private fun sortServicesByPriority(contexts: Map<String, List<ContextServiceManifest>>) {
        for ((_, list) in contexts) {
            list.sortedBy { it.getOrder() }
        }
    }

    private fun createVertxInstance(): Vertx {
        // todo 根据配置生成 VertxOptions
        val vertx = Vertx.vertx()
        Container.registerVertx(vertx)
        return vertx
    }

    private fun failure(error: Throwable) {
        log.error("启动失败", error)
    }

    private fun finish() {
        Container.finish()
        manager.clearCache()
        resolvers.clear()
        manifests.clear()
    }

}
