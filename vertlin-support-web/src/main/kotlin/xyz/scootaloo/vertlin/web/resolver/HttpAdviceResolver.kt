package xyz.scootaloo.vertlin.web.resolver

import xyz.scootaloo.vertlin.boot.resolver.ResourcesPublisher
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.web.HttpHandlerAdvice
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/31 下午10:37
 */
class HttpAdviceResolver : ServiceResolver(HttpHandlerAdvice::class) {

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
        TODO("Not yet implemented")
    }

}
