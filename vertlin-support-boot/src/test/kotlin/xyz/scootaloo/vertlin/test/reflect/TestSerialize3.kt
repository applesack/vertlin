package xyz.scootaloo.vertlin.test.reflect

import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import java.util.DoubleSummaryStatistics
import java.util.ListResourceBundle

/**
 * @author flutterdash@qq.com
 * @since 2022/7/22 下午4:45
 */
class TestSerialize3 : TestDSL {

    val f1: Map<String, Double> = HashMap()

    @Test
    fun test1() {
        val field = this::class.java.getDeclaredField("f1")
        TypeUtils.solveFieldGenericType(field).log()
    }

}
