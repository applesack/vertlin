package xyz.scootaloo.vertlin.boot.core

import xyz.scootaloo.vertlin.boot.util.Rearview
import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/7/21 上午8:18
 */
interface TestDSL {

    infix fun <T> T?.shouldBe(expect: T?) {
        if (this != expect) {
            throw ResultMistakeException(Rearview.formatCaller(5), this, expect)
        }
    }

    fun <T> T?.log() {
        if (this == null) {
            println("null")
        } else if (this is Array<*>) {
            println(Arrays.toString(this))
        } else {
            println(this)
        }
    }

    infix fun <T> T.check(block: (T) -> Unit) {
        block(this)
        "test pass".log()
    }

    class ResultMistakeException(caller: String, actual: Any?, expect: Any?) : RuntimeException(
        "`$actual` returns, but `$expect` expected\n at $caller"
    )

}
