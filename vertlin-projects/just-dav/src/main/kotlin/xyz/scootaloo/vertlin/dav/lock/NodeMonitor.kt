package xyz.scootaloo.vertlin.dav.lock

import xyz.scootaloo.vertlin.dav.domain.DepthHeader
import xyz.scootaloo.vertlin.dav.domain.TimeoutHeader

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 上午10:11
 */
sealed interface NodeMonitor : Iterable<NodeMonitor> {

    val size: Int

    fun parent(): NodeMonitor?

    fun child(name: String): NodeMonitor?

    fun delete(name: String)

    fun addChild(monitor: NodeMonitor)

    override fun iterator(): Iterator<NodeMonitor> {
        return emptyList<NodeMonitor>().iterator()
    }

}

class PlaceholderMonitor(val name: String) : NodeMonitor {

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun parent(): NodeMonitor? {
        TODO("Not yet implemented")
    }

    override fun child(name: String): NodeMonitor? {
        TODO("Not yet implemented")
    }

    override fun delete(name: String) {
        TODO("Not yet implemented")
    }

    override fun addChild(monitor: NodeMonitor) {
        TODO("Not yet implemented")
    }

}

class LockMonitor(
    val name: String,
    val token: String,
    val owner: String,
    val timeout: TimeoutHeader,
    val depth: DepthHeader
) : NodeMonitor {

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun parent(): NodeMonitor? {
        TODO("Not yet implemented")
    }

    override fun child(name: String): NodeMonitor? {
        TODO("Not yet implemented")
    }

    override fun delete(name: String) {
        TODO("Not yet implemented")
    }

    override fun addChild(monitor: NodeMonitor) {
        TODO("Not yet implemented")
    }

}
