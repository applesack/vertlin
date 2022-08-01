package xyz.scootaloo.vertlin.boot.resolver

import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.Order
import xyz.scootaloo.vertlin.boot.Ordered
import xyz.scootaloo.vertlin.boot.Service
import xyz.scootaloo.vertlin.boot.exception.NotSuspendMethodException
import xyz.scootaloo.vertlin.boot.internal.Constant
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午1:40
 */
abstract class ServiceResolver(val accept: KClass<out Service>) {

    @Throws(Throwable::class, NotSuspendMethodException::class)
    abstract fun solve(type: KClass<*>, manager: ResourcesPublisher)

    protected fun solveContext(klass: KClass<*>): String {
        return klass.findAnnotations(Context::class)
            .firstOrNull()?.value ?: return Constant.SYSTEM
    }

    protected fun solveOrder(klass: KClass<*>): Int {
        val order = klass.findAnnotations(Order::class)
            .firstOrNull()?.value ?: Ordered.DEFAULT
        return Ordered.suitable(order)
    }

}
