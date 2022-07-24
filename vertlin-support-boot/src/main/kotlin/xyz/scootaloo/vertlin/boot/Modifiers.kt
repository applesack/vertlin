package xyz.scootaloo.vertlin.boot

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午10:52
 */

annotation class Context(val value: String = "system")


annotation class Order(val value: Int)


object Ordered {

    const val LOWEST = 10

    const val DEFAULT = 5

    const val HIGHEST = 0

}
