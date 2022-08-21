package xyz.scootaloo.vertlin.boot.core

import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午9:46
 */
object X {

    fun getLogger(klass: KClass<*>): Logger {
        return LoggerFactory.getLogger(klass.java)
    }

    fun getLogger(name: String): Logger {
        return LoggerFactory.getLogger(name)
    }

}
