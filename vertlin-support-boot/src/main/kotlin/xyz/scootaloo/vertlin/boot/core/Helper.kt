package xyz.scootaloo.vertlin.boot.core

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.impl.logging.Logger
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.kotlin.coroutines.awaitResult

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午9:46
 */
interface Helper {

    fun getLogger(): Logger {
        return LoggerFactory.getLogger(this::class.java)
    }

    fun getLogger(name: String): Logger {
        return LoggerFactory.getLogger(name)
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

}
