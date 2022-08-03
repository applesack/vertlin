package xyz.scootaloo.vertlin.dav.domain

import kotlinx.serialization.Serializable
import java.util.ListResourceBundle

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午11:02
 */

@Serializable
data class LockBody(
    val isExclusive: Boolean,
    val owner: String
)

data class PropFindBody(
    val allProp: Boolean,
    val propName: Boolean,
    val prop: List<String>
)
