package xyz.scootaloo.vertlin.test.reflect

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.Json
import io.vertx.kotlin.core.json.array
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import kotlin.reflect.full.declaredMemberFunctions

/**
 * @author flutterdash@qq.com
 * @since 2022/7/21 下午2:59
 */
class TestSerialize : TestDSL {

    @Test
    fun test() {
        val function = this::class.declaredMemberFunctions.find { it.name == "consumer" }
        function.log()
    }

    @Test
    fun test2() {
        displayResult(arrayOf("abc", C(), 9.0, null))
        displayResult(arrayOf(hashMapOf("a" to 3), arrayOf("", 2, 4, "")))
    }

    private fun displayResult(args: Array<Any?>) {
        val toString = args.toString()
        val result = parse(args).toString()
        println("$toString =>> $result")
    }

    private fun parse(args: Array<Any?>): JsonArray {
        return JsonArray(args.asList())
//        return Json.array {
//            for (arg in args) {
//                when (arg) {
//                    null -> add(null)
//                    is Boolean, Byte, Short, Int, Float, Double, Long, String -> add(arg)
//                    is Array<*> -> JsonArray(arg.asList())
//                    is Collection<*> -> JsonArray(arg.toList())
//                    else -> {
//                        JsonObject.mapFrom(arg)
//                    }
//                }
//            }
//        }
    }

    @Suppress("unused")
    fun consumer(a: A, b: B, arr: List<Int>): C {
        return C()
    }

    data class A(val name: String)
    data class B(val key: Int)
    class C(val buck: Array<Int> = arrayOf(4, 5, 6)) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as C

            if (!buck.contentEquals(other.buck)) return false

            return true
        }

        override fun hashCode(): Int {
            return buck.contentHashCode()
        }
    }

}
