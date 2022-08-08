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

    @Test
    fun testPathUriEncode() {
        val text1 = "你好世界.txt"
        text1 shouldBe PathUtils.decodeUriComponent(PathUtils.encodeUriComponent(text1))

        val text2 = "你好 世界.txt"
        text2 shouldBe PathUtils.decodeUriComponent(PathUtils.encodeUriComponent(text2))

        val text3 = "你好+世界.txt"
        text3 shouldBe PathUtils.decodeUriComponent(PathUtils.encodeUriComponent(text3))

        val text4 = "笔记 2022年8月7日 14_40_25.txt"
        text4 shouldBe PathUtils.decodeUriComponent(PathUtils.encodeUriComponent(text4))

        val text5 = "/%E7%AC%94%E8%AE%B0+2022%E5%B9%B48%E6%9C%887%E6%97%A5+14_40_25.txt"
        PathUtils.decodeUriComponent(text5).log()

        val text6 = " +"
        PathUtils.encodeUriComponent(text6).log()

        val text7 = " %2b"
        PathUtils.decodeUriComponent(text7).log()
    }

}
