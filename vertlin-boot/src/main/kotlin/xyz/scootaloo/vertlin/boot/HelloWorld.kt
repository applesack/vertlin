package xyz.scootaloo.vertlin.boot

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午5:00
 */
object HelloWorld : BootManifest() {

    @JvmStatic
    fun main(args: Array<String>) {
        runApplication(args)
    }

}
