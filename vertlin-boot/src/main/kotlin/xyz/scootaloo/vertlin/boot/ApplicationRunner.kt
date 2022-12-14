package xyz.scootaloo.vertlin.boot

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.eventbus.EventBus
import io.vertx.core.file.FileSystem
import io.vertx.core.impl.logging.Logger
import xyz.scootaloo.vertlin.boot.command.CommandLineArgs
import xyz.scootaloo.vertlin.boot.config.ConfigProvider
import xyz.scootaloo.vertlin.boot.config.ConfigServiceResolver
import xyz.scootaloo.vertlin.boot.config.PropertySources
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.core.ifNotNull
import xyz.scootaloo.vertlin.boot.core.trans
import xyz.scootaloo.vertlin.boot.core.wrapInFut
import xyz.scootaloo.vertlin.boot.crontab.CrontabAdapter
import xyz.scootaloo.vertlin.boot.crontab.CrontabResolver
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApiResolver
import xyz.scootaloo.vertlin.boot.eventbus.EventbusCodecResolver
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.internal.Container
import xyz.scootaloo.vertlin.boot.internal.Extensions
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.resolver.ManifestReducer
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.resolver.impl.ManifestManagerImpl
import xyz.scootaloo.vertlin.boot.util.PackScanner
import xyz.scootaloo.vertlin.boot.util.StopWatch
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import xyz.scootaloo.vertlin.boot.verticle.MainVerticle
import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午5:10
 */
class ApplicationRunner(val boot: BootManifest) {

    private val log = X.getLogger(this::class)

    private val manager = ManifestManagerImpl

    private val resolvers = HashMap<String, ServiceResolver>()
    private val manifests = LinkedList<ContextServiceManifest>()

    private val monitor = Monitor(log, boot::class)

    /**
     * 1. 扫描扩展, 生成资源集合(主要是解析器和默认服务)
     * 2. 读取配置:
     *     1. 提取出配置相关的资源, 向配置中心中注册检查器(方便后面过滤不合规格的配置项)
     *     2. 从命令行或配置文件中读取配置, 根据profile信息(如果有)读取额外配置并覆盖原有配置
     *     3. 遍历配置服务相关资源, 尝试向配置中心注册缺省配置(优先级最低的配置, 只有预定配置项目不存在时才会生效)
     *     4. 检查是否有缺失的配置, 如果有必需的配置缺失, 则提前终止流程
     * 3. 处理命令行, 如果命令行不为空, 则找到对应的处理器和用户进行交互, 然后判断是否终止流程
     * 4. 处理解析器, 从资源集合中获取解析器的实现, 并使解析器和对应的处理类型绑定
     * 5. 处理服务集合, 遍历所有的服务类型, 将其交给对应的解析器处理, 解析器生成上下文服务清单或者单例资源发布到资源容器
     * 6. 处理配置清单集合, 遍历所有转换/约简器, 将同一类型或者多个类型转换成目标类型
     * 7. 所有服务解析完毕后, 资源容器将状态标记为锁定, 此时资源容器仅允许获取资源不准再注册新资源
     *    PS: 资源容器内的单例资源分为普通单例和上下文单例, 当前操作是禁止注册普通单例, 上下文单例注册的关闭延后到8.4
     * 8. 开启服务:
     *     1. 根据容器内的上下文服务清单, 创建对应的上下文容器实例
     *     2. 优先部署主控, 由主控优先初始化服务(根据配置清单内容), 然后依次部署其他上下文容器
     *         PS: 此时代码的执行从main线程跳转到了主控线程
     *     3. 其他上下文容器中初始化当前容器内的服务, 然后等待服务开启事件
     *     4. 主控发布服务开启事件, 同时等待所有的上下文容器的服务开启操作完成事件
     *     5. 其他上下文在服务开启操作完成后向主控发送服务开启完成状态
     *     6. 主控接受到所有的服务开启完成事件后, 服务开启完成
     * 9. 启动操作完毕, 标记各种容器的状态, 并清理上述所有步骤产生的缓存
     *     PS: 此时代码执行切回main线程
     *
     */
    fun run(args: Array<String>): Future<Unit> {
        return try {
            monitor.displayProjectInfo()
            val commandLine = CommandLineArgs.parse(args)
            loadResources(commandLine)
            if (!handleCommandLine(args)) {
                return Unit.wrapInFut()
            }

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

    private fun handleCommandLine(args: Array<String>): Boolean {
//        TODO()
        return true
    }

    private fun loadResources(cmd: CommandLineArgs) {
        monitor.displayBeforeServicesResolve()

        Container.start()
        loadResourcesFromCurrentProject()
        loadResourcesFromDependencies()
        loadResourcesFromPreset()

        val configProviders = Container.instancesOf(ConfigProvider::class).mapNotNull { it.second }
        PropertySources.loadConfigs(configProviders, cmd)

        loadResolvers(Container.instancesOf(ServiceResolver::class))
        loadServices(Container.instancesOf(Service::class))

        monitor.displayFinishServiceResolve()
    }

    private fun loadResourcesFromCurrentProject() {
        val resources = LinkedList<KClass<*>>()
        for (pack in boot.scanPacks()) {
            val classes = PackScanner.scan(pack)
            for (clazz in classes) {
                val klass = clazz.kotlin
                if (klass.isSubclassOf(Component::class) && !klass.isAbstract) {
                    resources.add(klass)
                }
            }
        }
        Container.loadResources(resources)
    }

    private fun loadResourcesFromPreset() {
        Container.registerResource(EventbusApiResolver)
        Container.registerResource(EventbusCodecResolver)
        Container.registerResource(CrontabResolver)
        Container.registerResource(ConfigServiceResolver)
    }

    private fun loadResourcesFromDependencies() {
        val extensions = Extensions.loadResources()
        Container.loadResources(extensions)
    }

    private fun loadResolvers(resolvers: Collection<Pair<KClass<*>, ServiceResolver?>>) {
        fun load(resolver: ServiceResolver, accept: KClass<*> = resolver.accept) {
            val typeQualified = TypeUtils.solveQualifiedName(accept)
            this.resolvers[typeQualified] = resolver
        }

        for ((_, resolver) in resolvers) {
            resolver.ifNotNull {
                load(it)
            }
        }

        load(CrontabResolver, CrontabAdapter::class)
    }

    private fun loadServices(services: Collection<Pair<KClass<*>, Service?>>) {
        fun load(type: KClass<*>, service: Service?) {
            for (superType in type.supertypes) {
                val superTypeName = superType.toString()
                if (superTypeName in resolvers) {
                    val resolver = resolvers[superTypeName]!!
                    resolver.solve(type, service, manager)
                    break
                }
            }
        }

        services.forEach { load(it.first, it.second) }

        val reducers = resolvers.values.filterIsInstance<ManifestReducer>()
        for (reducer in reducers) {
            manager.reduce(reducer)
        }

        manifests.addAll(manager.displayManifests())
    }

    private fun startServers(): Future<Vertx> {
        val contexts = manifests.groupBy { it.context() }.toMutableMap()
        val sysServices = contexts.remove(Constant.SYSTEM) ?: emptyList()
        sortServicesByPriority(contexts)

        // 将代码的执行权移交给MainVerticle

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
        Container.registerSharedSingleton(vertx, Vertx::class)
        Container.registerSharedSingleton(vertx.eventBus(), EventBus::class)
        Container.registerSharedSingleton(vertx.fileSystem(), FileSystem::class)
        return vertx
    }

    private fun failure(error: Throwable) {
        log.error("启动失败", error)
        Container.failure()
        clearCache()
    }

    private fun finish() {
        clearCache()
        monitor.displayFinishBootstrap()
    }

    private fun clearCache() {
        manager.clearCache()
        resolvers.clear()
        manifests.clear()
    }

    private class Monitor(val log: Logger, val boot: KClass<*>) {

        private val stopWatch = StopWatch()

        companion object {
            private const val BOOT = "boot"
            private const val RESOLVE = "resolve"
        }

        fun displayProjectInfo() {
            val builder = StringBuilder()
            builder.append("Starting ${boot.simpleName} ")
            val machine = InetAddress.getLocalHost().hostName
            builder.append("on $machine ")
            val pid = ManagementFactory.getRuntimeMXBean().name.split('@')[0]
            builder.append("with PID $pid ")
            val pwd = System.getenv("PWD")
            val username = System.getenv("USERNAME")
            builder.append("($pwd by $username)")
            log.info(builder)
            stopWatch.start(BOOT)
        }

        fun displayBeforeServicesResolve() {
            stopWatch.start(RESOLVE)
        }

        fun displayFinishServiceResolve() {
            val time = (stopWatch.stop(RESOLVE) * 1000.0).toLong()
            log.info("Service Resolver: resolve finished in $time ms")
        }

        fun displayFinishBootstrap() {
            val time = stopWatch.stop(BOOT)
            val runningTime = (ManagementFactory.getRuntimeMXBean().uptime) / 1000.0
            log.info("Started ${boot.simpleName} in $time seconds (JVM running for $runningTime)")
        }

        fun displayFailureBootstrap() {
        }

    }

}
