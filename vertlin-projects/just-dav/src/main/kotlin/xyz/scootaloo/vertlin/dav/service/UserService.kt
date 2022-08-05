package xyz.scootaloo.vertlin.dav.service

import xyz.scootaloo.vertlin.boot.eventbus.EventbusApi
import xyz.scootaloo.vertlin.boot.internal.inject
import xyz.scootaloo.vertlin.dav.application.WebDAV

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午5:27
 */
class UserService : EventbusApi() {

    private val config by inject(WebDAV::class)

    @Accept(String::class)
    @Ret(String::class)
    val findPassByName = api {
        val username = it.asPojo<String>()
        config.users[username] ?: ""
    }

}
