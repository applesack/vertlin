package xyz.scootaloo.vertlin.test.reflect

import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import xyz.scootaloo.vertlin.boot.util.TypeUtils
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * @author flutterdash@qq.com
 * @since 2022/7/22 上午10:31
 */
class TestSerialize2 : TestDSL {

    @Test
    fun test0() {
        val method = this::class.java.declaredMethods.first { it.name == "method1" }
        val g = method.genericParameterTypes
        method.parameterTypes.indices.forEach {
            TypeUtils.solveParamGenericType(method, g, it).log()
        }
    }

    private fun method1(array: List<Double>, map: LinkedHashMap<String, Float>) {
    }

    @Test
    fun testArray() {
        val array = arrayOf("abc", "cfd")
        val klass = array::class
        klass.log()
    }

    @Test
    fun testList() {
        val list = listOf("abc", "def")
        val klass = list::class
        klass.log()
    }

    @Test
    fun testObject() {
        val any = A()
        val jsonObject = JsonObject.mapFrom(any)
        val json = jsonObject.mapTo(A::class.java)
        json.log()
    }

    @Test
    fun testCollection() {
        val method = this::class.java.declaredMethods.first { it.name == "method2" }
        val argList = method.parameterTypes
        argList.log()
    }

    @Test
    fun testGeneric() {
        val b = B()
        b.names = listOf("a", "b", "c")
        b.map = hashMapOf("name" to "case", "age" to "13")
        val jo = JsonObject.mapFrom(b)
        jo.toString().log()
        val obj = jo.mapTo(B::class.java)
        obj.log()
    }

    @Test
    fun testGeneric2() {
        val b = B()
        b.names = listOf("a", "b", "c")
        b.map = hashMapOf("name" to "case", "age" to "13")
        val klass = b::class
        val kFields = klass.memberProperties
        val fields = klass.java.declaredFields
        fields[0].genericType
        fields.log()
        kFields.log()
    }

    @Test
    fun testPrimaryConstructor() {
        val klass = C::class
        val cons = klass.primaryConstructor
        cons.log()
    }

    private fun method2(list: List<A>, map: HashMap<String, Pair<A, Int>>) {

    }

    data class A(
        val name: String = "asd",
        val age: Int = 12
    )

    data class C(
        val name: String, val age: Int,
        val array: Array<Float>,
        val list: List<Long>,
        val mapper: Map<Int, String>,
        val obj: C
    )

    class B {
        lateinit var names: List<String>
        lateinit var map: HashMap<String, String>
        override fun toString(): String {
            return "B(names=$names, map=$map)"
        }
    }

}
