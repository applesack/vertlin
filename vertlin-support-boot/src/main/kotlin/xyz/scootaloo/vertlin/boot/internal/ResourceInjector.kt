package xyz.scootaloo.vertlin.boot.internal

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 下午2:25
 */

fun inject(context: String): Lazy<CoroutineResource> {
    return CoroutineResourceInjector(context)
}


private class CoroutineResourceInjector(val context: String) : Lazy<CoroutineResource> {

    @Volatile private var coroutine: CoroutineResource? = null

    override val value: CoroutineResource
        get() {
            val tmp = coroutine
            if (tmp != null) {
                return tmp
            }

            coroutine = Container.getCoroutineRes(context)
            return coroutine!!
        }

    override fun isInitialized(): Boolean {
        return coroutine != null
    }

}
