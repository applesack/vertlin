package xyz.scootaloo.vertlin.boot.exception

/**
 * @author flutterdash@qq.com
 * @since 2022/7/20 上午10:03
 */

open class SystemException(msg: String, cause: Throwable?) : RuntimeException(msg, cause)


class DeployServiceException(serviceName: String, context: String, cause: Throwable?) : SystemException(
    "部署服务异常: 服务名'$serviceName', 在上下文'$context'中;", cause
)


class RequiredConfigLackException(item: String, msg: String) : SystemException(
    "配置项异常: 缺失必要的配置项'$item'; $msg", null
)
