package xyz.scootaloo.vertlin.dav.service

import xyz.scootaloo.vertlin.boot.eventbus.EventbusApi

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午5:27
 */
class UserService : EventbusApi() {

    val findPassByName = api {
        true
    }

}
