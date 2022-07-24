package xyz.scootaloo.vertlin.test.reflect

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test

/**
 * @author flutterdash@qq.com
 * @since 2022/7/22 下午3:57
 */
class TestJackson {

    private val objectMapper = ObjectMapper()

    @Test
    fun test() {
    }

    class Student(
        val name: String,
        val age: Int,
    )

}
