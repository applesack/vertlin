package xyz.scootaloo.vertlin.boot.core

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.awaitResult
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


infix fun <T> Promise<T>.complete(value: T?) {
    this.complete(value)
}

fun <T> T.wrapInFut(): Future<T> {
    return Future.succeededFuture(this)
}

fun <T, R> Future<T>.trans(lazy: (T) -> R): Future<R> {
    return this.transform { done ->
        if (done.succeeded()) {
            Future.succeededFuture(lazy(done.result()))
        } else {
            Future.failedFuture(done.cause())
        }
    }
}

suspend fun <T> awaitParallelBlocking(block: () -> T): T {
    return awaitResult { handler ->
        val ctx = Vertx.currentContext()
        ctx.executeBlocking({ fut ->
            fut complete block()
        }, false, { ar ->
            handler.handle(ar)
        })
    }
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

    companion object {

        fun <T : Any> nothing(): LazyValue<T> {
            return LazyValue { throw UnsupportedOperationException() }
        }

    }

}
