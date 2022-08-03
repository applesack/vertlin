## vertlin

用于快速开发`vertx`应用的脚手架, `springboot like`

快速启动, 线程模型,`profile`, `eventbus`, 自动注入, 定时任务, 本地缓存



## 线程模型

事件循环单线程服务器有很高效的执行效率, 而且有效地解决了传统多线程下对象并发访问的问题. `vertx`在此基础上引入了上下文的概念, 通常情况下一个线程(`EventLoop`)对应了一个上下文

- `HttpRouter`: 当有请求访问指定的路径时, 回调被触发
- `EventbusConsumer`: 事件总线的接收者, 路径上有消息时回调被触发
- `Crontab`: 定时任务, 每个任务可以动态地调整下一次调用的时机, 当满足调用条件则任务被执行

考虑以上几种情况, 一个应用由若干个回调组成, 如果这样的回调很多, 且都部署在同一个上下文中时, 会使得请求的响应速度变慢, 为了提高执行效率需要把这些回调合理地部署在多个上下中.  所以这个脚手架的主要做的工作是更方便的将回调部署到特定的上下文, 且更流畅地跨上下文调用接口. 

在本框架中, 能提供功能的回调称为服务(`Service`), 一个服务只能存在于一个上下文中, 而创建上下文在`vertx`需要使用`verticle`. 如果你需要开发一个文件管理系统, 它有以下结构:

- `MainVerticle`: 用于部署其他`verticle`, 读取配置, 管理状态, 管理用户
- `ServerVerticle`: 用于处理`HTTP`请求并响应, 处理权限
- `FileServiceVerticle`:  提供文件服务
- `Worker`: 耗时任务, 例如请求体的序列化和反序列化

你可能会这样写代码:

```kotlin
class MainVerticle : CoroutineVerticle() {
    override suspend fun start() {
        // 在这里初始化或注册一些服务
    }
}

vertx.deployVerticle(MainVerticle())
	.compose { vertx.deployVerticle(ServerVerticle()) }
	.compose { vertx.deployVerticle(FileServiceVerticle()) }
```



在本框架中, 没有`verticle`, 只有服务. 每个服务都是一个类, 而类上的注解会指定了类中的代码会在哪个上下文中执行, 而且可以使用统一的接口让一个服务在其他服务之间共享(类似于`springboot`的`autowire`)


> 得益于`kotlin`协程, 可以用同步的写法去写异步代码, 脚手架(框架)大量使用了协程


## 从HelloWorld开始

一个最简单的demo, 用于演示编写一个简单的`http`服务器, 对于任意请求都返回`hello world`

引入依赖(`gradle`)

`implementation "xyz.scootaloo:vertlin-boot:0.1" // 核心组件`

`implementation "xyz.scootaloo:vertlin-web:0.1" // web支持`

这行划掉, 短期内不考虑发布到中央仓库

1. 编写启动类
```kotlin
import xyz.scootaloo.vertlin.boot.BootManifest

object Launcher : BootManifest() {
	@JvmStatic
	fun main(args: Array<String>) {
		runApplication(args)
	}
}
```
启动类继承于`BootManifest`, 这个类有一些默认方法, 可以帮助启动器确定要加载哪些资源, 默认情况下(不重写任何方法), 会递归扫描当前包下所有的class文件, 找到目标实现类并作为资源载入, 可以重写这个类的方法更改加载资源的逻辑.

2. 编写路由器
```kotlin
import io.vertx.ext.web.Router
import xyz.scootaloo.vertlin.web.HttpRouterRegister

// 处理所有以"/"开头的路径
class HelloHttpRouter : HttpRouterRegister("/*") {
	override fun register(router: Router) = router.run {
        // 处理使用get访问以"/"开头的请求
		get("/*") { // 这里可以使用协程
			it.end("hello world")
		}
	}
}
```
这样一个路由器就写完了. 执行`Launcher`的main方法, 服务器启动, 回到浏览器输入`http://localhost:8080`可以看到输出的`hello world`


## 创建HTTP服务器

在`hello world`示例中编写了基本的`http`服务器, 这里是更详细地介绍一些其他功能

1. 监听指定方法访问路径的请求

```kotlin
// HttpRouterRegister 实现类中
// 提供了常用的方法get post head options delete trace connect put patch
get("/test/:name") { ctx ->
    val name = ctx.pathParam("name")
    delay(2000) // 在这里暂停2秒, 模拟一个异步调用
    ctx.end(name) // 2秒后返回捕获到的路径参数
}
post("/test") {
    it.end()
}
put("/test") {
    it.end()
}
// 路径可以是正则表达式, 参考vertx文档
```

2. 编写过滤器/拦截器

```kotlin
// 实现一个简单的权限拦截器
@Order(Ordered.HIGHEST) // 指定优先级, 这个路由可以优先被装配
class AuthInterceptor : HttpRouterRegister("/*") {

    override fun register(router: Router) = router.run {
        any { // 任意路径的请求都会经过这个拦截器
            val auth = it.request().getHeader("Authentication")
            val result = handle(ctx, auth).await() // 异步校验权限
            if (result) {
                // 如果检查通过则放行
                it.next()
            } else {
                it.fail(401) // 返回一个错误状态码
            }
        }
    }

}
```

3. 修改服务器的参数

服务器默认监听端口8080, 如果要修改这个配置, 在`resources`目录下创建一个`config.toml`文件, 文件内容

```toml
[http]
port = 9090
```

这样再次启动, 端口就监听到9090了

框架为`http`服务器准备了一些默认参数, 完整配置如下

```
[http]
port = 8080
bodyLimit = -1
deleteUploadedFilesOnEnd = true
uploadsDirectory = "uploads"

[websocket]
maxWebSocketFrameSize = 1024
maxWebSocketMessageSize = 4918
```



## WebSocket

todo



## 使用配置



## 自动注入



## Eventbus



## 本地缓存



## 高级主题

### 服务与服务清单

### 服务解析器

### 服务的生命周期

### 命令行

