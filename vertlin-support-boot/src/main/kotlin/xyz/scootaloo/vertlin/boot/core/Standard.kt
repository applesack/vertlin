package xyz.scootaloo.vertlin.boot.core

import xyz.scootaloo.vertlin.boot.internal.Constant

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午9:30
 */

fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

inline fun <T : Any> T?.ifNotNull(block: (T) -> Unit): T? {
    if (this != null) {
        block(this)
    }
    return this
}

infix fun String.like(other: String): Boolean {
    if (this.length != other.length)
        return false
    return this.startsWith(other, true)
}


/**
 * 延迟加载的值, 只有第一次使用[value]的时候值会被载入
 *
 * 该类型没有同步锁保护, 所以不支持在多线程环境下使用
 */
class LazyValue<T : Any>(
    private val init: () -> T
) {
    private var flag = Constant.PRESET
    val value: T
        get() {
            if (flag === Constant.PRESET) {
                flag = init()
            }
            @Suppress("UNCHECKED_CAST")
            return flag as T
        }
}
