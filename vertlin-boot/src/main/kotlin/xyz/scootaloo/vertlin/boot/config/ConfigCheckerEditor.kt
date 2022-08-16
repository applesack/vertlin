package xyz.scootaloo.vertlin.boot.config

import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/8/15 下午10:13
 */
interface ConfigCheckerEditor {

    fun <T : Any> key(k: String, type: KClass<T>, init: Checker<T>.() -> Unit = {})

    interface Checker<T> {

        fun range(verify: (T) -> Boolean)

        fun rangeTips(tips: String)

        fun required()

    }

}
