package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:09
 */
interface EventbusConsumer {

    fun address(): String

    fun handle(request: Message<JsonArray>)

}
