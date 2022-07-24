package xyz.scootaloo.vertlin.boot.util

import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.exception.CreateInstanceException
import xyz.scootaloo.vertlin.boot.internal.Constant
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午10:01
 */
object TypeUtils : Helper {

    private val log = getLogger()

    internal fun initialize() {
        TypeFactory.initialize()
    }

    fun solveQualifiedName(type: KClass<*>, def: String = Constant.EMPTY_STR): String {
        return type.qualifiedName ?: return kotlin.run {
            log.warn("限定名异常: 类型'$type'的限定名为空")
            def
        }
    }

    fun solveParamGenericType(source: Any, types: Array<Type>, idx: Int): List<KClass<*>> {
        return solveGenericType(source, types[idx])
    }

    fun solveFieldGenericType(field: Field): List<KClass<*>> {
        val generic = field.genericType
        return solveGenericType(field, generic)
    }

    @Throws(CreateInstanceException::class)
    @Deprecated("暂时未使用过")
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

    private fun solveGenericType(source: Any, type: Type): List<KClass<*>> {
        if (type is ParameterizedType) {
            val actualTypes = type.actualTypeArguments
            return actualTypes.map { TypeFactory.getType(it.typeName) }
        } else {
            throw RuntimeException("反射异常: 无法获取域'$source'的泛型信息")
        }
    }

    private object TypeFactory {

        private val mapperLock = ReentrantReadWriteLock()
        private val mapper = HashMap<String, KClass<*>>()

        fun initialize() {
            mapperLock.write {
                put(Boolean::class)
                put(Byte::class)
                put(Short::class)
                put(Int::class)
                put(Float::class)
                put(Double::class)
                put(Long::class)
                put(String::class)
            }
        }

        fun getType(typeName: String): KClass<*> {
            val klass = mapperLock.read {
                mapper[typeName]
            }
            if (klass != null) {
                return klass
            }

            val result = runCatching { loadKClass(typeName) }
            return mapperLock.write { put(result.getOrThrow()) }
        }

        private fun put(type: KClass<*>): KClass<*> {
            mapper[solveQualifiedName(type)] = type
            return type
        }

        private fun loadKClass(name: String): KClass<*> {
            if (name in mapper) {
                return CUtils.notnullGet(mapper, name)
            }
            val clazz = Class.forName(name)
            return clazz.kotlin
        }

    }

}
