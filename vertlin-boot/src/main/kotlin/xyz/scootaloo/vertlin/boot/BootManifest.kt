package xyz.scootaloo.vertlin.boot

import io.vertx.core.Future
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.PackScanner
import java.util.*
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午10:40
 */
abstract class BootManifest {

    fun runApplication(args: Array<String>): Future<Unit> {
        configLogger()
        return ApplicationRunner(this).run(args)
    }

    open fun scanPacks(): List<String> {
        return listOf(this::class.java.packageName)
    }

    private val cache by lazy { ResourcesCache(scanPacks()) }

    open fun services(): Collection<KClass<out Service>> {
        return cache.getServices()
    }

    open fun resolvers(): Collection<KClass<out ServiceResolver>> {
        return cache.getResolvers()
    }

    private fun configLogger() {
        // logback的一个bug, 当依赖的jar中有logback.xml时会无法解析内容
        // 使用下面的配置指定xml文档解析工厂, 这个配置需要尽快调用(早于第一个日志输出)
        System.getProperties().setProperty(
            "javax.xml.parsers.SAXParserFactory",
            "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
        )
        // https://www.cnblogs.com/ki16/p/15587460.html
    }

    class ResourcesCache(private val packs: List<String>) {

        private var hasInit = false

        private val services = LinkedList<KClass<out Service>>()
        private val resolvers = LinkedList<KClass<out ServiceResolver>>()

        fun getServices(): Collection<KClass<out Service>> {
            if (hasInit) return services
            initialize()
            return services
        }

        fun getResolvers(): Collection<KClass<out ServiceResolver>> {
            if (hasInit) return resolvers
            initialize()
            return resolvers
        }

        @Suppress("UNCHECKED_CAST")
        private fun initialize() {
            for (pack in packs) {
                PackScanner.scan(pack).forEach {
                    if (Service::class.java.isAssignableFrom(it)) {
                        val kotlin = it.kotlin
                        if (!kotlin.isAbstract) {
                            services.add(kotlin as KClass<out Service>)
                        }
                    }
                    if (ServiceResolver::class.java.isAssignableFrom(it)) {
                        val kotlin = it.kotlin
                        if (!kotlin.isAbstract) {
                            resolvers.add(kotlin as KClass<out ServiceResolver>)
                        }
                    }
                }
            }

            hasInit = true
        }

    }

}
