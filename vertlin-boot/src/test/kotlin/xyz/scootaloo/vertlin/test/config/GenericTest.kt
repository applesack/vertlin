package xyz.scootaloo.vertlin.test.config

import org.junit.jupiter.api.Test
import java.util.LinkedList

/**
 * @author flutterdash@qq.com
 * @since 2022/8/16 上午8:07
 */
class GenericTest {

    @Test
    fun test() {
        val numberChecker = Checker<Int> { println(it + 1) }
        val stringChecker = Checker<String> { println(it.repeat(3)) }
        val booleanChecker = Checker<Boolean> { println(!it) }

        val checkers = LinkedList<Checker<Any>>()
        checkers.add(numberChecker as Checker<Any>)
        checkers.add(stringChecker as Checker<Any>)
        checkers.add(booleanChecker as Checker<Any>)

        val params = arrayListOf<Any>(13, "abc", false)
        for (idx in params.indices) {
            val param = params[idx]
            val checker = checkers[idx]
            checker.accept(param)
        }
    }

    fun interface Checker<T> {

        fun accept(value: T)

    }

}
