package xyz.scootaloo.vertlin.boot

import io.vertx.core.Future
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import kotlin.reflect.KClass

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午10:40
 */
interface BootManifest {

    fun services(): Collection<KClass<out Service>>

    fun resolvers(): Collection<ServiceResolver> = emptyList()

    fun runApplication(args: Array<String>): Future<Unit> {
        return ApplicationRunner.run(this, args)
    }

}
