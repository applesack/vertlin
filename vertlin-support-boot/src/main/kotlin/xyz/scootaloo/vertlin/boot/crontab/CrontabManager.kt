package xyz.scootaloo.vertlin.boot.crontab

import xyz.scootaloo.vertlin.boot.ContextOnly
import xyz.scootaloo.vertlin.boot.InjectableService

/**
 * @author flutterdash@qq.com
 * @since 2022/7/25 上午10:10
 */
interface CrontabManager : InjectableService, ContextOnly {

    fun publishCrontab(crontab: Crontab)

}
