package xyz.scootaloo.vertlin.boot

import io.vertx.core.Future
import io.vertx.core.Vertx
import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApiResolver
import xyz.scootaloo.vertlin.boot.exception.InheritanceRelationshipException
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.Factory
import xyz.scootaloo.vertlin.boot.resolver.ServiceReducer
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.CUtils
import xyz.scootaloo.vertlin.boot.util.SysUtils
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import xyz.scootaloo.vertlin.boot.verticle.MainVerticle
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.system.exitProcess

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午5:10
 */
object ApplicationRunner : Helper {

    private val log = getLogger()

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
                    val cause = done.cause()
                    failure(cause)
                }
            }
        } catch (error: Throwable) {
            failure(error)
        }
    }

    private fun readCommandLine(args: Array<String>): Boolean {
//        TODO()
        return true
    }

    private fun prepareStarting() {
        Container.start()
        SysUtils.initialize()
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
        val typeMapper = HashMap<String, MutableList<ContextServiceManifest>>()
        val reducers = resolvers.values.filterIsInstance<ServiceReducer<*>>()
            .associateBy { TypeUtils.solveQualifiedName(it.acceptSourceType()) }

        for (service in services) {
            var solved = false
            for (superType in service.supertypes) {
                val superTypeName = superType.toString()
                if (superTypeName in resolvers) {
                    val resolver = resolvers[superTypeName]!!
                    val manifest = resolver.solve(service)

                    if (superTypeName in reducers) {
                        CUtils.grouping(typeMapper, manifest.context(), manifest) { ArrayList() }
                    } else {
                        manifests.add(manifest)
                    }

                    if (manifest is Factory) {
                        val instance = manifest.getObject()
                        Container.registerObject(manifest.instanceType(), instance)
                    }

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

        for ((type, list) in typeMapper) {
            val reducer = CUtils.notnullGet(reducers, type)
            val reduceManifest = reducer.reduce(list)
            manifests.add(reduceManifest)
        }
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

    private fun failure(error: Throwable): Nothing {
        log.error("启动失败", error)
        exitProcess(0)
    }

    private fun finish() {
        Container.finish()
        resolvers.clear()
        manifests.clear()
    }

}
