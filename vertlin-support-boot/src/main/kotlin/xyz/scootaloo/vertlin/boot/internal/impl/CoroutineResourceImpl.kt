package xyz.scootaloo.vertlin.boot.internal.impl

import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.scootaloo.vertlin.boot.internal.CoroutineResource

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:22
 */
class CoroutineResourceImpl(
    private val coroutineVerticle: CoroutineVerticle
) : CoroutineResource {

    override fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineVerticle.launch {
            block()
        }
    }

}
