package xyz.scootaloo.vertlin.test.cglib

import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

/**
 * @author flutterdash@qq.com
 * @since 2022/7/18 上午9:48
 */
class ObjectClassProxy {

    @Test
    fun test() {
        val singleton = enhancer()
        println(singleton.a1())
        println(singleton.a2())
    }

    private fun enhancer(): Singleton {
        val enhancer = Enhancer()
        enhancer.setSuperclass(Singleton::class.java)
        enhancer.setCallback(Interrupter())
        return enhancer.create() as Singleton
    }

    class Singleton {

        fun a1(): String {
            return "a1"
        }

        fun a2(): Int {
            return 2
        }

    }

    class Interrupter : MethodInterceptor {
        override fun intercept(
            obj: Any, method: Method, args: Array<out Any>, proxy: MethodProxy
        ): Any {
            return proxy.invokeSuper(obj, args)
        }
    }

}
