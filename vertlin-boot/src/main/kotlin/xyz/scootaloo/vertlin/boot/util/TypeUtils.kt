package xyz.scootaloo.vertlin.boot.util

import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.exception.CreateInstanceException
import xyz.scootaloo.vertlin.boot.internal.Constant
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午10:01
 */
object TypeUtils {

    private val log = X.getLogger(this::class)

    fun solveQualifiedName(type: KClass<*>, def: String = Constant.EMPTY_STR): String {
        return type.qualifiedName ?: return kotlin.run {
            log.warn("限定名异常: 类型'$type'的限定名为空")
            def
        }
    }

    @Throws(CreateInstanceException::class)
    fun createInstanceByNonArgsConstructor(klass: KClass<*>): Any {
        for (constructor in klass.constructors) {
            if (constructor.typeParameters.isEmpty()) {
                if (!constructor.isAccessible) {
                    constructor.isAccessible = true
                }
                val result = kotlin.runCatching {
                    constructor.call()
                }
                return result.getOrElse {
                    throw CreateInstanceException(klass, it)
                }
            }
        }

        throw CreateInstanceException(klass, null)
    }

}
