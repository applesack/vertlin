package xyz.scootaloo.vertlin.boot.exception

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午3:50
 */

open class ReflectionException(msg: String, cause: Throwable?) : RuntimeException(msg, cause)


class CreateInstanceException(type: KClass<*>, cause: Throwable?) : ReflectionException(
    "反射异常: 创建实例时遇到错误, 在'${type.qualifiedName}'; 请考虑为此类型设置无参构造器", cause
)


class CreateSubclassException(type: KClass<*>, cause: Throwable?) : ReflectionException(
    "反射异常: 创建目标类型的子类时遇到错误, 在'${type.qualifiedName}'; 清尝试将此类型的修饰符修改为open, 使其允许继承", cause
)
