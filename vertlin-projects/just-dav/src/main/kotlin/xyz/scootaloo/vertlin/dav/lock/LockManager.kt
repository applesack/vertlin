package xyz.scootaloo.vertlin.dav.lock

import xyz.scootaloo.vertlin.boot.Context
import xyz.scootaloo.vertlin.boot.eventbus.EventbusApi
import xyz.scootaloo.vertlin.dav.domain.IfHeader
import xyz.scootaloo.vertlin.dav.domain.LockBlock

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午3:20
 */
@Context("file")
class LockManager : EventbusApi() {

    private val root = PlaceholderMonitor("/")

    val lock = api {
        val lock = it.asPojo<LockBlock>()
        val lockDiscovery = getOrCreate(lock)
        encode(lockDiscovery)
    }

    val refreshLock = api {
        val lockBlock = it.asPojo<LockBlock>()
        ""
    }

    val unlock = api {
        encode("")
    }

    /**
     * 只处理深度为1或者无限的情况
     * 如果入参目标点被锁且无法解锁, 则返回元组中第一个值永远为false
     */
    @Acc("Triple<String, IfHeader?, Int>", desc = "目标点, if条件, 深度")
    @Ret("Pair<String, List<String>>", desc = "该点是否允许访问, 指定深度范围内的所有存在锁的点")
    val detect = api {
        it.asPojo<Triple<String, IfHeader?, Int>>()
        encode(Pair(true, emptyList<String>()))
    }

    private fun getOrCreate(lock: LockBlock): LockDiscovery {
        val pathItems = lock.target.split('/')
        var current: NodeMonitor = root
        var idx = 0
        while (idx < pathItems.size - 1) {
            val item = pathItems[idx]
            var child = current.child(item)
            if (child == null) {
                val new = PlaceholderMonitor(item)
                current.addChild(new)
                child = new
            } else if (child is LockMonitor) {
                if (!hasPermissions(child, lock, false)) {
                    return buildLockDiscovery(child)
                }
            }

            current = child
            idx++
        }
        TODO()
    }

    private fun hasPermissions(lock: LockMonitor, block: LockBlock, hit: Boolean): Boolean {
        if (!hit && lock.depth.depth == 0) {
            return true
        }

        return evaluate(lock, block)
    }

    private fun evaluate(lock: LockMonitor, block: LockBlock): Boolean {
        throw UnsupportedOperationException()
    }

    private fun buildLockDiscovery(lock: LockMonitor): LockDiscovery {
        throw UnsupportedOperationException()
    }

}
