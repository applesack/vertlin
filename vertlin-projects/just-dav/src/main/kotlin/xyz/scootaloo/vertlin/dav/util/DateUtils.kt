package xyz.scootaloo.vertlin.dav.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午4:39
 */
object DateUtils {

    private val ZONE_GMT = ZoneId.of("GMT")
    private val rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME

    fun gmt(time: Long): String {
        val instant = Date(time).toInstant()
        return LocalDateTime.ofInstant(instant, ZONE_GMT).toString()
    }

    fun rfc1123(date: Long): String {
        return rfc1123.format(Instant.ofEpochMilli(date).atZone(ZONE_GMT))
    }

}
