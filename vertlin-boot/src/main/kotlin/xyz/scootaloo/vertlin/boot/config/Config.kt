package xyz.scootaloo.vertlin.boot.config

import xyz.scootaloo.vertlin.boot.InjectableService

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午10:21
 */
interface Config : InjectableService


annotation class Prefix(val value: String = "")
