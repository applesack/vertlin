package xyz.scootaloo.vertlin.boot.util

import kotlinx.serialization.decodeFromString
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

    inline fun <reified T> encode(value: T): String {
        return Json.encodeToString(value)
    }

    inline fun <reified T> decode(value: String): T {
        return Json.decodeFromString(value)
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
