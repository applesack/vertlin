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



## 创建HTTP服务器



## WebSocket



## 使用配置



## 自动注入



## Eventbus



## 本地缓存



## 高级主题

### 服务与服务清单

### 服务解析器

### 服务的生命周期

### 命令行

