package xyz.scootaloo.vertlin.boot.crontab

import xyz.scootaloo.vertlin.boot.Service

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午10:44
 */
interface Crontab : Service {

    val id: String

    var valid: Boolean

    var delay: Long

    var order: Int

    fun run(currentTimeMillis: Long)

}
