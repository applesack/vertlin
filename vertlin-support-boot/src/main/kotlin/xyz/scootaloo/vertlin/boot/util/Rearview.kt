package xyz.scootaloo.vertlin.boot.util

/**
 * @author flutterdash@qq.com
 * @since 2022/7/21 上午8:19
 */
object Rearview {

    fun formatCaller(deep: Int): String {
        return format(caller(deep))
    }

    private fun caller(deep: Int): StackTraceElement {
        return Thread.currentThread().stackTrace[deep]
    }

    private fun format(trace: StackTraceElement): String {
        return "${trace.className}(${trace.fileName}:${trace.lineNumber})"
    }

}
