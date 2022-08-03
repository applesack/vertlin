package xyz.scootaloo.vertlin.dav.lock

import kotlinx.serialization.Serializable

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午2:36
 */
@Serializable
class LockDiscovery(
    val lockType: String,
    val lockScope: String,
    val depth: String,
    val owner: String,
    val timeout: String,
    val lockToken: String?,
    val lockRoot: String
)
