package xyz.scootaloo.vertlin.hello

import xyz.scootaloo.vertlin.boot.BootManifest

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午9:15
 */
object Launcher : BootManifest() {

    @JvmStatic
    fun main(args: Array<String>) {
        runApplication(args)
    }

}
