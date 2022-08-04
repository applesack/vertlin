package xyz.scootaloo.vertlin.dav.file

/**
 * @author flutterdash@qq.com
 * @since 2022/8/4 上午9:04
 */
enum class State(val code: Int) {

    OK(200),
    FORBIDDEN(403),
    NOT_FOUND(404),
    ERROR(500)

}
