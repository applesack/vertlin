package xyz.scootaloo.vertlin.dav.parse.header

import xyz.scootaloo.vertlin.boot.util.Encoder
import xyz.scootaloo.vertlin.dav.file.FileInfo
import xyz.scootaloo.vertlin.dav.util.PathUtils
import java.util.regex.Pattern

/**
 * @author flutterdash@qq.com
 * @since 2022/8/10 上午10:12
 */
object DestinationHeaderParser {

    private val regx: Pattern by lazy {
        Pattern.compile("((\\w+)://([^/:]+)(:\\d*)?)")
    }

    /**
     * 解析请求中的Destination头, 提取目的信息, 如果找不到这样的信息则返回null
     *
     * 它的格式可能是这样的 `http://192.168.0.1:8080/abc`
     * 但是本系统是为了访问目的机器上的文件系统设计的(不考虑跨机器),
     * 所以只截取部分内容`/abc`
     *
     * 如果客户端通过域名访问, 拿到的格式可能类似于 `http://sample.com/abc`
     *
     * 也可能是这样, 只包含目标路径, 而不携带主机信息 `/abc`
     *
     * 另外, 需要手动对目的路径进行转码
     */
    fun parseDest(content: String?): String? {
        val notnull = content ?: return null
        val matcher = regx.matcher(notnull)
        if (!matcher.find()) {
            return decodeUriComponent(notnull)
        }

        val matched = runCatching { matcher.group(0) }
        if (matched.isFailure) {
            return null
        }
        val urlPrefix = matched.getOrThrow()
        return decodeUriComponent(notnull.substring(urlPrefix.length))
    }

    private fun decodeUriComponent(content: String): String {
        val decoded = PathUtils.decodeUriComponent(content)
        return FileInfo.normalize(decoded)
    }

}
