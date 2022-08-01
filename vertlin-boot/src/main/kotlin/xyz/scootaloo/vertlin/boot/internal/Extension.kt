package xyz.scootaloo.vertlin.boot.internal

import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.config.ConfigProvider
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.CUtils
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午8:53
 */
internal object Extension {

    private val log = X.getLogger(this::class)

    private const val RES_LOCATION = "META-INF/ext.txt"

    // 接口名 -> 实现类的类型列表
    private val typeMapper = HashMap<String, MutableList<KClass<*>>>()

    // 类型名 -> 实例
    private val instanceMapper = HashMap<String, Any>()

    private val roots = listOf(
        ServiceResolver::class, Service::class, ConfigProvider::class
    )

    fun initialize(loader: ClassLoader) {
        val classNameSet = HashSet<String>()
        for (url in loader.getResources(RES_LOCATION)) {
            val lines = readLines(url)
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue
                }
                classNameSet.add(line.trim())
            }
        }

        for (className in classNameSet) {
            val result = kotlin.runCatching {
                loader.loadClass(className)
            }
            if (result.isSuccess) {
                val klass = result.getOrThrow().kotlin
                for (root in roots) {
                    if (klass.isSubclassOf(root)) {
                        val superTypeName = TypeUtils.solveQualifiedName(root)
                        CUtils.grouping(typeMapper, superTypeName, klass) { LinkedList() }
                    }
                }
            } else {
                log.warn("加载类错误: 配置文件'$RES_LOCATION', 类名'$className'")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> instances(type: KClass<T>): List<T> {
        val superTypeName = TypeUtils.solveQualifiedName(type)
        val types = typeMapper[superTypeName] ?: emptyList()
        return types.map build@{ klass ->
            val typeQName = TypeUtils.solveQualifiedName(klass)
            if (typeQName in instanceMapper) {
                return@build instanceMapper[typeQName]!! as T
            }

            val instance = TypeUtils.createInstanceByNonArgsConstructor(klass)
            instanceMapper[typeQName] = instance
            instance as T
        }
    }

    fun types(type: KClass<*>): List<KClass<*>> {
        val typeQName = TypeUtils.solveQualifiedName(type)
        return typeMapper[typeQName] ?: emptyList()
    }

    fun clear() {
        typeMapper.clear()
        instanceMapper.clear()
    }

    private fun readLines(url: URL): List<String> {
        val bufferReader = BufferedReader(InputStreamReader(url.openStream()))
        val result = bufferReader.readLines()
        bufferReader.close()
        return result
    }

}
