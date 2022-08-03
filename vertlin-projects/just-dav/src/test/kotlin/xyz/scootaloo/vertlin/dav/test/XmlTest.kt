package xyz.scootaloo.vertlin.dav.test

import org.dom4j.DocumentHelper
import org.junit.jupiter.api.Test

/**
 * @author flutterdash@qq.com
 * @since 2022/8/1 下午4:17
 */
class XmlTest {

    @Test
    fun test() {
        val xml = """
            <?xml version="1.0" encoding="utf-8" ?>
     <D:lockinfo xmlns:D='DAV:'>
       <D:lockscope><D:exclusive/></D:lockscope>
       <D:locktype><D:write/></D:locktype>
       <D:owner>
         <D:href>http://example.org/~ejw/contact.html</D:href>
       </D:owner>
     </D:lockinfo>
        """.trimIndent()

        val document = DocumentHelper.parseText(xml)!!
        val prefix = document.rootElement.namespacePrefix ?: ""
        val selected = document.selectNodes("/D:lockinfo/D:lockscope[1]")
        println(selected)
    }

}
