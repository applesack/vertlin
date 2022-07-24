package xyz.scootaloo.vertlin.boot.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:20
 */
interface CoroutineResource {

    fun launch(block: suspend CoroutineScope.() -> Unit): Job

}
