package xyz.scootaloo.vertlin.boot

import io.vertx.core.Future

/**
 * @author flutterdash@qq.com
 * @since 2022/7/19 上午10:40
 */
abstract class BootManifest {

    fun runApplication(args: Array<String>): Future<Unit> {
        configLogger()
        return ApplicationRunner(this).run(args)
    }

    open fun scanPacks(): List<String> {
        return listOf(this::class.java.packageName)
    }

    private fun configLogger() {
        // logback的一个bug, 当依赖的jar中有logback.xml时会无法解析内容
        // 使用下面的配置指定xml文档解析工厂, 这个配置需要尽快调用(早于第一个日志输出)
        System.getProperties().setProperty(
            "javax.xml.parsers.SAXParserFactory",
            "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"
        )
        // https://www.cnblogs.com/ki16/p/15587460.html
    }

}
