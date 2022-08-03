package xyz.scootaloo.vertlin.dav.domain

import kotlinx.serialization.Serializable

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 下午4:55
 */
@Serializable
data class LockBlock(
    val target: String,
    val condition: IfHeader?,
    val depth: DepthHeader,
    val timeout: TimeoutHeader,
    val body: LockBody
)


@Serializable
data class PropFindBlock(
    val target: String,
    val depth: DepthHeader
)
