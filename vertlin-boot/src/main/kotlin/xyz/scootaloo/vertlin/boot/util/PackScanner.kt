package xyz.scootaloo.vertlin.boot.util

import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.jar.JarFile

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午8:52
 */
object PackScanner {

    fun scan(pack: String): Set<Class<*>> {
        var packageName = pack
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.lastIndexOf('.'))
        }

        val basePackageFilePath = packageName.replace('.', '/')

        val classes = LinkedHashSet<Class<*>>()
        val resources = contentClassLoader().getResources(basePackageFilePath)
        for (resource in resources) {
            val protocol = resource.protocol
            if (protocol == "file") {
                val filePath = URLDecoder.decode(resource.file, "UTF-8")
                doScanPackageClassesByFile(classes, packageName, filePath)
            } else if (protocol == "jar") {
                doScanPackageClassesByJar(classes, resource, packageName)
            }
        }

        return classes
    }

    private fun doScanPackageClassesByFile(
        receiver: MutableSet<Class<*>>, packageName: String, packagePath: String
    ) {
        val dir = File(packagePath)
        if (!dir.exists() || !dir.isDirectory) {
            return
        }

        val dirContents = dir.listFiles filter@{ file ->
            if (file.isDirectory) {
                return@filter true
            }

            val filename = file.name

            if (!filename.endsWith(".class")) {
                return@filter false
            }

            !filename.contains('$')
        } ?: return

        for (file in dirContents) {
            if (file.isDirectory) {
                doScanPackageClassesByFile(
                    receiver, "$packageName.${file.name}", file.absolutePath
                )
            } else {
                val className = file.name.substring(0, file.name.length - 6)
                val qualifiedName = "$packageName.$className"
                val loadClass = runCatching {
                    contentClassLoader().loadClass(qualifiedName)
                }
                if (loadClass.isSuccess) {
                    receiver.add(loadClass.getOrThrow())
                }
            }
        }
    }

    private fun doScanPackageClassesByJar(
        receiver: MutableSet<Class<*>>, url: URL, basePackage: String
    ) {
        val basePackageFilePath = basePackage.replace('.', '/')
        val jarFile: JarFile = (url.openConnection() as JarURLConnection).jarFile
        for (entry in jarFile.entries()) {
            val name = entry.name
            if (!name.startsWith(basePackageFilePath) || entry.isDirectory) {
                continue
            }

            if ('$' in name) {
                continue
            }

            val className = name.replace('/', '.')
            val qualifiedName = className.substring(0, className.length - 6)
            val loadClass = runCatching {
                contentClassLoader().loadClass(qualifiedName)
            }
            if (loadClass.isSuccess) {
                receiver.add(loadClass.getOrThrow())
            }
        }
    }

    private fun contentClassLoader(): ClassLoader {
        return Thread.currentThread().contextClassLoader
    }

}
