package xyz.scootaloo.vertlin.boot.util

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午11:08
 */
object Encoder {

    private val md5 = MessageDigest.getInstance("MD5")
    private const val HEX = "0123456789abcdef"

    private val base64encoder = Base64.getEncoder()
    private val base64decoder = Base64.getDecoder()

    fun md5(content: String): String {
        val bytes = md5.digest(content.toByteArray())
        return bytes2hex(bytes)
    }

    fun base64encode(content: String): String {
        return base64encoder.encodeToString(content.encodeToByteArray())
    }

    fun base64decode(encoded: String): String {
        return String(base64decoder.decode(encoded))
    }

    /**
     * @return 原始类型, 字符串, JsonArray, JsonObject
     */
    fun simpleEncode2Json(value: Any): Any {
        return when (value) {
            is List<*> -> JsonArray(value)
            is Array<*> -> JsonArray(value.toList())
            is String -> Json.encodeToString(value)
            is Boolean -> Json.encodeToString(value)
            is Byte -> Json.encodeToString(value)
            is Short -> Json.encodeToString(value)
            is Int -> Json.encodeToString(value)
            is Float -> Json.encodeToString(value)
            is Double -> Json.encodeToString(value)
            is Long -> value
            else -> {
                JsonObject.mapFrom(value)
            }
        }
    }

    private fun bytes2hex(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            builder.append(HEX[b.toInt() shr 4 and 0x0f])
            builder.append(HEX[b.toInt() and 0x0f])
        }
        return builder.toString()
    }

}
