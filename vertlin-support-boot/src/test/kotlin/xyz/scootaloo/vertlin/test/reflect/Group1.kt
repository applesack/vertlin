package xyz.scootaloo.vertlin.test.reflect

import org.junit.jupiter.api.Test
import xyz.scootaloo.vertlin.boot.core.TestDSL
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.system.measureTimeMillis

/**
 * 速度统计; 调用方法十万次, 观察消耗时间 (单位毫秒), 每种调用方式测试5次
 *
 * 直接调用     java反射调用     java缓存反射调用        kotlin反射调用         kotlin缓存反射调用
 *   [7]           63               29                    333                    42
 *   [8]           54               27                    341                    57
 *   [10]          53               29                    365                    36
 *   [10]          49               31                    327                    47
 *   [9]           53               27                    319                    45
 * 可见性优化后
 *                57               [19]                   370                    30
 *                64               [18]                   363                    53
 *                62               [16]                   329                    37
 *                58               [27]                   406                    53
 *                57               [16]                   351                    36
 *
 * @author flutterdash@qq.com
 * @since 2022/7/21 上午8:14
 */
class Group1 : TestDSL {

    private val count = 100000

    @Test
    fun test1() {
        val target = Target()
        measureTimeMillis {
            repeat(count) {
                target.testFun4(it)
            }
        }.log()
    }

    @Test
    fun test2() {
        val target = Target()
        val clazz = target::class.java
        measureTimeMillis {
            repeat(count) {
                val method = clazz.getDeclaredMethod("testFun4", Int::class.java)
                method.isAccessible = true
                method.invoke(target, it)
            }
        }.log()
    }

    @Test
    fun test3() {
        val target = Target()
        val clazz = target::class.java
        val method = clazz.getDeclaredMethod("testFun4", Int::class.java)
        method.isAccessible = true
        measureTimeMillis {
            repeat(count) {
                method.invoke(target, it)
            }
        }.log()
    }

    @Test
    fun test4() {
        val target = Target()
        val klass = target::class
        measureTimeMillis {
            repeat(count) {
                val func = klass.declaredMemberFunctions.first { it1 ->
                    it1.name == "testFun4"
                }
                func.isAccessible = true
                func.call(target, it)
            }
        }.log()
    }

    @Test
    fun test5() {
        val target = Target()
        val klass = target::class
        val function = klass.declaredMemberFunctions.first { it1 ->
            it1.name == "testFun4"
        }
        function.isAccessible = true
        measureTimeMillis {
            repeat(count) {
                function.call(target, it)
            }
        }.log()
    }

}

class Target {

    fun testFun1(number: Int): Int {
        return number
    }

    fun testFun2(number: Int): Int {
        return number
    }

    fun testFun3(number: Int): Int {
        return number
    }

    fun testFun4(number: Int): Int {
        var count = 0
        repeat(100) {
            count++
        }
        return count
    }

    fun testFun5(number: Int): Int {
        return number
    }

}
