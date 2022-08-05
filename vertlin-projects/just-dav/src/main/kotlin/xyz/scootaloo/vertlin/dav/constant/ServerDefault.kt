package xyz.scootaloo.vertlin.dav.constant

import xyz.scootaloo.vertlin.dav.domain.DepthHeader
import xyz.scootaloo.vertlin.dav.domain.TimeoutHeader

/**
 * @author flutterdash@qq.com
 * @since 2022/8/2 上午12:12
 */
object ServerDefault {

    const val serverPort = 8080

    val timeout = TimeoutHeader(10 * 1000L, false)

    val depth = DepthHeader(1, false)

    const val realm = "just-dav"

    const val privateKey = "FlyMeToTheMoon"

}
