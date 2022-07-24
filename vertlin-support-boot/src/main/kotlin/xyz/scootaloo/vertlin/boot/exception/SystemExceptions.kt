package xyz.scootaloo.vertlin.boot.exception

import java.util.*

/**
 * @author flutterdash@qq.com
 * @since 2022/7/20 上午10:03
 */

open class SystemException(msg: String, cause: Throwable?) : RuntimeException(msg, cause)


class DeployServiceException(serviceName: String, context: String, cause: Throwable?) : SystemException(
    "部署服务异常: 服务名'$serviceName', 在上下文'$context'中;", cause
)


class EventbusApiArgumentException(address: String, args: Array<out Any?>) : SystemException(
    "事件总线接口异常: 参数错误, 目标地址'$address', 使用参数'${args.contentToString()}'", null
)


class EventbusApiInvokeException(address: String, args: Array<out Any?>) : SystemException(
    "事件总线接口异常: 方法内部错误, 目标地址'$address', 使用参数'\${args.contentToString()", null
)
