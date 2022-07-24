package xyz.scootaloo.vertlin.boot.eventbus

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.JsonObject
import xyz.scootaloo.vertlin.boot.resolver.ContextServiceManifest
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 上午8:40
 */

class JsonCodec<R : Any>(
    internal val type: KClass<R>,
    private val convert: (JsonObject) -> R
) : ContextServiceManifest {

    internal fun decode(json: JsonObject): R {
        return convert(json)
    }

    internal fun toMessageCodec(): MessageCodec<R, R> {
        return JsonMessageDecoder(type, this)
    }

    override fun name(): String {
        throw UnsupportedOperationException()
    }

    override fun context(): String {
        throw UnsupportedOperationException()
    }

    override suspend fun register(vertx: Vertx) {
        throw UnsupportedOperationException()
    }

}


private class JsonMessageDecoder<R : Any>(
    type: KClass<R>,
    private val codec: JsonCodec<R>
) : MessageCodec<R, R> {

    private val coderName = TypeUtils.solveQualifiedName(type)

    override fun encodeToWire(buffer: Buffer, s: R) {
        val encoded = JsonObject.mapFrom(s).toBuffer()
        buffer.appendInt(encoded.length())
        buffer.appendBuffer(encoded)
    }

    override fun decodeFromWire(pos: Int, buffer: Buffer): R {
        val length = buffer.getInt(pos)
        val offset = length + 4
        val json = JsonObject(buffer.slice(offset, offset + length))
        return codec.decode(json)
    }

    override fun name(): String {
        return coderName
    }

    override fun systemCodecID(): Byte {
        return -1
    }

    override fun transform(s: R): R {
        return s
    }

}
