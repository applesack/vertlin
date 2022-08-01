package xyz.scootaloo.vertlin.boot.crontab

import xyz.scootaloo.vertlin.boot.Ordered

/**
 * @author flutterdash@qq.com
 * @since 2022/7/24 下午10:47
 */
abstract class CrontabAdapter : Crontab {

    override var valid = CrontabDefault.defValid

    override var delay = CrontabDefault.defDelay

    override var order = CrontabDefault.defOrder

}

object CrontabDefault {

    const val defValid = true

    const val defDelay = 100L

    const val defOrder = Ordered.DEFAULT

}
