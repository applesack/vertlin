package xyz.scootaloo.vertlin.boot.util

import xyz.scootaloo.vertlin.boot.core.ifNotNull
import java.io.File
import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.jar.JarFile

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午8:52
 */
object PackScanner {

    fun scan(pack: String): Set<Class<*>> {
        val classes = LinkedHashSet<Class<*>>()
        val packDir = pack.replace('.', '/')

        try {
            val dirs = loadPackDirs(packDir)
            for (url in dirs) {
                when (url.protocol) {
                    "file" -> handleFileResource(url, pack, classes)
                    "jar" -> handleJarResource(url, classes)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return classes
    }

    private fun handleFileResource(url: URL, packName: String, classes: MutableSet<Class<*>>) {
        val filePath = URLDecoder.decode(url.file, "UTF-8")
        findAndAddClassesInPackageByFile(packName, filePath, true, classes)
    }

    private fun handleJarResource(
        url: URL, classes: MutableSet<Class<*>>
    ) {
        val jar = openJarConnection(url)
        for (entry in jar.entries()) {
            val name = purePath(entry.name)
            if (name.endsWith(".class") && !entry.isDirectory) {
                val className = extractClassName(name)
                val packName = name.replace('/', '.')
                safeLoadClass("${packName}.$className").ifNotNull {
                    classes.add(it)
                }
            }
        }
    }

    private fun safeLoadClass(fullClassName: String): Class<*>? {
        return runCatching { loader().loadClass(fullClassName) }.getOrNull()
    }

    private fun openJarConnection(url: URL): JarFile {
        return (url.openConnection() as JarURLConnection).jarFile
    }

    private fun extractClassName(fullName: String): String {
        val delimiter = fullName.lastIndexOf('/')
        return fullName.substring(delimiter + 1, fullName.length - ".class".length)
    }

    private fun purePath(path: String): String {
        val pre = if (path.startsWith("/")) path.substring(1) else path
        return if (pre.endsWith("/")) pre.substring(0, pre.length) else pre
    }

    private fun findAndAddClassesInPackageByFile(
        packageName: String,
        packagePath: String,
        recursive: Boolean,
        classes: MutableSet<Class<*>>
    ) {
        val dir = File(packagePath)
        if (!dir.exists() || !dir.isDirectory) {
            return
        }

        val dirFiles = dir.listFiles { file ->
            (recursive && file.isDirectory || file.name.endsWith(".class"))
        } ?: return

        for (file in dirFiles) {
            if (file.isDirectory) {
                findAndAddClassesInPackageByFile(
                    "$packageName.${file.name}", file.absolutePath, recursive, classes
                )
            } else {
                val fileName = file.name
                val className = fileName.substring(0, fileName.length - ".class".length)
                classes.add(loadClass(packageName, className))
            }
        }
    }

    private fun loadPackDirs(packDir: String): Enumeration<URL> {
        return loader().getResources(packDir)
    }

    private fun loadClass(packName: String, klassName: String): Class<*> {
        return Class.forName("${packName}.$klassName")
    }

    private fun loader(): ClassLoader {
        return Thread.currentThread().contextClassLoader
    }

}
