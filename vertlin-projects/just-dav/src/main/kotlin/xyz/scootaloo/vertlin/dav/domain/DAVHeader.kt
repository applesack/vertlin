package xyz.scootaloo.vertlin.dav.domain

import kotlinx.serialization.Serializable

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午11:04
 */
@Serializable
class DepthHeader(
    val depth: Int,
    val noRoot: Boolean
)


@Serializable
class IfHeader(
    val tagged: String?,
    val subConditions: List<ConditionGroup>
)


@Serializable
class TimeoutHeader(
    val amount: Long,
    val infinite: Boolean
)


@Serializable
class ETag(
    val weak: Boolean,
    val name: String
)


@Serializable
class ConditionGroup(
    val eTagConditions: ArrayList<ETagCondition> = ArrayList(),
    val tokenConditions: ArrayList<TokenCondition> = ArrayList()
)


@Serializable
class ETagCondition(
    val not: Boolean,
    val eTag: ETag
)


@Serializable
class TokenCondition(
    val not: Boolean,
    val token: String
)
