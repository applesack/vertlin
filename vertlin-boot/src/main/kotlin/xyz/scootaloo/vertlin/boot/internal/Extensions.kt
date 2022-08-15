package xyz.scootaloo.vertlin.boot.internal

import xyz.scootaloo.vertlin.boot.Component
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
internal object Extensions {

    private const val RES_LOCATION = "META-INF/extensions.txt"

    fun loadResources(): List<KClass<*>> {
        val loader = loader()
        val classNameSet = HashSet<String>()
        for (url in loader.getResources(RES_LOCATION)) {
            val lines = readLines(url)
            for (line in lines) {
                val pure = line.trim()
                if (line.isEmpty() || pure.startsWith('#')) {
                    continue
                }

                classNameSet.add(pure)
            }
        }

        val resources = LinkedList<KClass<*>>()
        for (name in classNameSet) {
            val result = runCatching { loader.loadClass(name) }
            if (result.isSuccess) {
                val clazz = result.getOrThrow()
                val klass = clazz.kotlin
                if (klass.isSubclassOf(Component::class) && !klass.isAbstract) {
                    resources.add(klass)
                }
            }
        }

        return resources
    }

    private fun loader(): ClassLoader {
        return Thread.currentThread().contextClassLoader
    }

    private fun readLines(url: URL): List<String> {
        val bufferReader = BufferedReader(InputStreamReader(url.openStream()))
        val result = bufferReader.readLines()
        bufferReader.close()
        return result
    }

}
