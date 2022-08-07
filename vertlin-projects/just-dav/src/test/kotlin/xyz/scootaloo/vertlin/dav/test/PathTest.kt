package xyz.scootaloo.vertlin.dav.test

import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import xyz.scootaloo.vertlin.dav.util.PathUtils
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.system.measureTimeMillis

/**
 * @author flutterdash@qq.com
 * @since 2022/8/3 下午11:47
 */
class PathTest : TestDSL {

    @Test
    fun testRelative() {
        val base = Path("/home/twi")
        val other = Path("/home/twi/dev/jdks")
        base.relativize(other).log()
    }

    @Test
    fun testUriEncode() {
        val path = "/你好/世界.txt"
        val encoded = PathUtils.encodeUriComponent(path)
        encoded.log()
        val decoded = PathUtils.decodeUriComponent(encoded)
        decoded.log()
    }

}
