package xyz.scootaloo.vertlin.boot

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午5:00
 */
object HelloWorld : BootManifest {

    @JvmStatic
    fun main(args: Array<String>) {
        runApplication(args)
    }

    override fun services(): Collection<KClass<Service>> {
        return emptyList()
    }

}
