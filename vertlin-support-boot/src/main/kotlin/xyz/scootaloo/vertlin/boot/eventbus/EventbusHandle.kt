package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午9:10
 */
interface EventbusHandle<T> {

    suspend operator fun invoke(json: JsonObject = jsonObjectOf()): T

    suspend fun request(json: JsonObject = jsonObjectOf()): Message<T>

}
