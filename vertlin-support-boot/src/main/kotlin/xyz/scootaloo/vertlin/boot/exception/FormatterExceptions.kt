package xyz.scootaloo.vertlin.boot.exception

import java.lang.IllegalArgumentException
import java.util.FormatFlagsConversionMismatchException
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午6:18
 */

open class FormatterException(msg: String) : IllegalArgumentException(msg)


class NotSuspendMethodException(klass: KClass<*>, method: KFunction<*>) : FormatterException(
    "方法格式异常: 目标方法'${klass.qualifiedName}#${method.name}'不是suspend方法; 需要将该方法修饰为suspend"
)


class AbstractMethodException(klass: KClass<*>, method: KFunction<*>) : FormatterException(
    "方法格式异常: 不支持的抽象方法, 在'${klass.qualifiedName}#${method.name}'"
)


class InheritanceRelationshipException(klass: KClass<*>, target: KClass<*>) : FormatterException(
    "类型继承关系错误: 类型'$klass'必须直接或间接继承于'$target'"
)
