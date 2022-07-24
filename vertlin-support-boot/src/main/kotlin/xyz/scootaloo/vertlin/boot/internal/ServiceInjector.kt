package xyz.scootaloo.vertlin.boot.internal

import xyz.scootaloo.vertlin.boot.util.TypeUtils
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午11:59
 */

fun <T : Any> inject(s: KClass<T>): Lazy<T> {
    return ServiceInjector(s)
}


/**
 * 由于这里是从线程安全的容器里获取对象, 且容器里保存的对象永远不会被覆盖,
 * 所有可以保证在不加锁的情况下每次调用都返回同一个实例, 而不会额外创建
 */
private class ServiceInjector<T : Any>(
    val klass: KClass<*>
) : Lazy<T> {

    @Volatile private var _value = Constant.PRESET

    override val value: T
        get() {
            val tmp = _value
            if (tmp !== Constant.PRESET) {
                @Suppress("UNCHECKED_CAST")
                return tmp as T
            }

            // 这里没有进行加锁, 所以当两个线程同时第一次访问实例的时候, 都会尝试去容器获取
            // 获取动作会发生两次, 但是从第二次开始访问实例时, 就不会再访问容器了

            _value = getObject(klass)

            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun isInitialized(): Boolean {
        return _value !== Constant.PRESET
    }

    companion object {

        private fun getObject(type: KClass<*>): Any {
            return Container.getObjectByQualifiedName(TypeUtils.solveQualifiedName(type))!!
        }

    }

}
