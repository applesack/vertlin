package xyz.scootaloo.vertlin.boot.util

/**
 * @author flutterdash@qq.com
 * @since 2022/7/22 下午4:52
 */
internal object SysUtils {

    fun initialize() {
        TypeUtils.initialize()
        Json2Kotlin.initialize()
    }

}
