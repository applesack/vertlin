package xyz.scootaloo.vertlin.boot.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import xyz.scootaloo.vertlin.boot.ContextOnly

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:20
 */
interface CoroutineResource : ContextOnly {

    fun launch(block: suspend CoroutineScope.() -> Unit): Job

}
