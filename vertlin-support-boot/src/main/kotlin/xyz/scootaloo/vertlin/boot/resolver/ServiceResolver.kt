package xyz.scootaloo.vertlin.boot.resolver

import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.exception.NotSuspendMethodException
import xyz.scootaloo.vertlin.boot.internal.Constant
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午1:40
 */
abstract class ServiceResolver {

    abstract fun acceptType(): KClass<*>

    @Throws(Throwable::class, NotSuspendMethodException::class)
    abstract fun solve(type: KClass<*>): ContextServiceManifest

    protected fun solveContext(klass: KClass<*>): String {
        return klass.findAnnotations(Context::class)
            .firstOrNull()?.value ?: return Constant.SYSTEM
    }

}
