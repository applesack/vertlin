package xyz.scootaloo.vertlin.boot.eventbus.impl

import io.vertx.core.json.JsonArray
import io.vertx.kotlin.coroutines.await
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import xyz.scootaloo.vertlin.boot.exception.EventbusApiArgumentException
import xyz.scootaloo.vertlin.boot.exception.EventbusApiInvokeException
import xyz.scootaloo.vertlin.boot.internal.Container
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction
import xyz.scootaloo.vertlin.boot.util.Json2Kotlin

/**
 * @author flutterdash@qq.com
 * @since 2022/7/17 下午11:32
 */
class EventbusApiInvokeInterceptor(private val addressPrefix: String) : MethodInterceptor {

    private val eventbus by lazy { Container.getVertx().eventBus() }
    private val coroutineFunction by lazy {
        for (func in this::class.java.declaredMethods) {
            if (func.name == "coroutineEntry") {
                func.isAccessible = true
                return@lazy func
            }
        }
        throw RuntimeException()
    }

    override fun intercept(
        obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy
    ): Any? {
        // todo 上下文检查, 如果上下文相同, 则直接在当前线程执行
        if (method.name == "invoke") {
            return proxy.invokeSuper(obj, args)
        }

        val function = method.kotlinFunction!!
        val targetAddress = Json2Kotlin.qualifiedAddressByMethod(addressPrefix, function)
        return coroutineHandle(args.last()) {
            val arguments = Json2Kotlin.serializeArguments(args.take(args.size - 1))
            val resp = eventbus.request<Any>(targetAddress, arguments).await()
            val respBody = resp.body() as JsonArray

            val state = respBody.getInteger(0)
            if (state == 1) {
                throw EventbusApiArgumentException(targetAddress, args)
            }
            if (state == 2) {
                throw EventbusApiInvokeException(targetAddress, args)
            }

            val methodResult = respBody.getValue(1)
            Json2Kotlin.deserializeReturnValue(methodResult, method)
        }
    }

    @Suppress("unused")
    suspend fun coroutineEntry(block: suspend () -> Any): Any {
        return block()
    }

    private fun coroutineHandle(coroutine: Any, block: suspend () -> Any?): Any? {
        return coroutineFunction.invoke(this, block, coroutine)
    }

}
