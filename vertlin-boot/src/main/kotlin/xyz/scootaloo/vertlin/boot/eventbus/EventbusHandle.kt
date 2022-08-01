package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午9:10
 */
interface EventbusHandle<T> {

    /**
     * 括号操作符的重载, 通过括号表达式直接获取一个异步调用的返回值
     */
    suspend operator fun invoke(params: JsonObject = jsonObjectOf()): T

    /**
     * 获取异步调用的结果
     */
    suspend fun request(params: JsonObject = jsonObjectOf()): Message<T>

}
