package xyz.scootaloo.vertlin.dav

import xyz.scootaloo.vertlin.boot.BootManifest

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 上午11:32
 */
object Launcher : BootManifest() {

    @JvmStatic
    fun main(args: Array<String>) {
        runApplication(args)
    }

}
