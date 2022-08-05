package xyz.scootaloo.vertlin.dav.test

import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import xyz.scootaloo.vertlin.dav.util.Encoder

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 下午11:26
 */
class EncodeTest : TestDSL {

    @Test
    fun testEncoder() {
        val content = "fly me to the moon"
        val encoded = Encoder.base64encode(content)
        encoded.log()
        val decoded = Encoder.base64decode(encoded)
        decoded.log()
    }

}
