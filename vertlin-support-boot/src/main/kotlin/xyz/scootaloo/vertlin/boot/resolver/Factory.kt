package xyz.scootaloo.vertlin.boot.resolver

import xyz.scootaloo.vertlin.boot.exception.ReflectionException
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午4:20
 */
interface Factory {

    fun instanceType(): KClass<*> {
        throw UnsupportedOperationException()
    }

    @Throws(ReflectionException::class)
    fun getObject(): Any

}
