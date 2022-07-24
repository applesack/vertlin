package xyz.scootaloo.vertlin.boot.eventbus.impl

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import xyz.scootaloo.vertlin.boot.core.Helper
import xyz.scootaloo.vertlin.boot.eventbus.EventbusConsumer
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.boot.util.Json2Kotlin
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:14
 */
class EventbusConsumerImpl(
    context: String,
    private val instance: Any,
    private val address: String,
    private val method: KFunction<*>
) : EventbusConsumer, Helper {

    private val log = getLogger(address)

    init {
        method.isAccessible = true
    }

    private val coroutine by inject(context)
    private val javaMethod = method.javaMethod!!

    override fun address(): String {
        return address
    }

    override fun handle(request: Message<JsonArray>) {
        val rawParams = request.body()
        val parseResult = runCatching { solveArguments(request.body()) }
        if (parseResult.isFailure) {
            log.error(
                "事件总线错误: 解析入参时, 参数'$rawParams'",
                parseResult.exceptionOrNull()
            )
            val fail = JsonArray(listOf(1, null))
            request.reply(fail)
            return
        }

        coroutine.launch {
            val invokeResult = runCatching {
                // todo 将参数列表展平
                method.callSuspend(instance)
//                method.callSuspend(flatten(parseResult.getOrThrow()))
            }
            if (invokeResult.isFailure) {
                log.error(
                    "事件总线错误: 执行目标方法时遇到异常, 在'$method', 使用参数'$rawParams'",
                    invokeResult.exceptionOrNull()
                )
                val fail = JsonArray(listOf(2, null))
                request.reply(fail)
                return@launch
            }

            val returnValue = invokeResult.getOrThrow()
            val serialized = Json2Kotlin.serializeReturnValue(returnValue)
            val succeed = JsonArray(listOf(0, serialized))
            request.reply(succeed)
        }
    }

    private fun solveArguments(json: JsonArray): Array<Any?> {
        return Json2Kotlin.deserializeArguments(json, javaMethod, javaMethod.parameterCount - 1)
    }

    private fun flatten(args: Array<Any?>): Array<Any?> {
        if (args.isEmpty()) return arrayOf(instance)
        return Array(args.size + 1) { idx ->
            if (idx == 0) {
                instance
            } else {
                args[idx - 1]
            }
        }
    }

}
