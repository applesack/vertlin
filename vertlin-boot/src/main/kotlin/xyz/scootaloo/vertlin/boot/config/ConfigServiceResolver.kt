package xyz.scootaloo.vertlin.boot.config

import com.moandjiezana.toml.Toml
import xyz.scootaloo.vertlin.boot.command.CommandLineArgs
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.exception.RequiredConfigLackException
import xyz.scootaloo.vertlin.boot.internal.Constant
import xyz.scootaloo.vertlin.boot.resolver.ResourcesPublisher
import xyz.scootaloo.vertlin.boot.resolver.ServiceResolver
import xyz.scootaloo.vertlin.boot.util.CUtils
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor

/**
 * @author flutterdash@qq.com
 * @since 2022/7/27 下午3:41
 */
object ConfigServiceResolver : ServiceResolver(Config::class) {

    private val log = X.getLogger(this::class)

    override fun solve(type: KClass<*>, manager: ResourcesPublisher) {
        val result = createInstance(type)
        if (result.isFailure) {
            throw TypeMismatchingException(type, result.exceptionOrNull())
        }

        val instance = result.getOrThrow()
        manager.publishSharedSingleton(instance)
    }

    internal fun load(
        loader: ClassLoader, cmd: CommandLineArgs,
        configProviders: Collection<ConfigProvider>
    ) {
        Center.load(loader, cmd, configProviders)
    }

    private fun createInstance(type: KClass<*>): Result<Any> {
        return kotlin.runCatching {
            val prefixAnno = type.findAnnotations(Prefix::class)
            val prefix = if (prefixAnno.isNotEmpty()) {
                prefixAnno.first().value
            } else {
                ""
            }

            val primaryConstructor = type.primaryConstructor!!
            val typeList = primaryConstructor.javaConstructor!!.parameterTypes
            val params = primaryConstructor.parameters
            val args = HashMap<KParameter, Any?>()
            for (idx in params.indices) {
                val param = params[idx]
                val paramType = typeList[idx]
                val configName = if (prefix.isNotEmpty())
                    "${prefix}.${param.name}" else param.name!!
                args[param] = simpleConvert(paramType.kotlin, getProperty(configName))
            }

            primaryConstructor.isAccessible = true
            primaryConstructor.callBy(args)
        }
    }

    private fun simpleConvert(type: KClass<*>, value: Any?): Any? {
        val notnull = value ?: return null
        return when (type) {
            Boolean::class -> notnull.toString().toBoolean()
            Short::class -> (notnull as Number).toShort()
            Int::class -> (notnull as Number).toInt()
            Float::class -> (notnull as Number).toFloat()
            Double::class -> (notnull as Number).toDouble()
            Long::class -> (notnull as Number).toLong()
            String::class -> notnull.toString()
            else -> notnull
        }
    }

    private fun getProperty(name: String): Any? {
        return Center.getProperties(name)
    }

    private object Store : ConfigManager {

        val defValues = HashMap<String, Any>()
        val checkers = HashMap<String, (Any) -> Boolean>()
        val requires = HashMap<String, String>()

        override fun registerDefault(key: String, value: Any) {
            defValues[key] = value
        }

        override fun registerChecker(key: String, check: (Any) -> Boolean) {
            checkers[key] = check
        }

        override fun registerRequired(key: String, msg: String) {
            requires[key] = msg
        }

    }

    private object Center {

        private const val profilesFilePrefix = "profiles.prefix"
        private const val profilesActive = "profiles.active"

        private val properties = HashMap<String, Any>()

        fun load(
            loader: ClassLoader, cmd: CommandLineArgs,
            configProviders: Collection<ConfigProvider>
        ) {
            configProviders.forEach { it.register(Store) }
            loadAndSelectProfile(loader, cmd)
            loadConfig(Store.defValues, false)

            for ((require, msg) in Store.requires) {
                if (getProperties(require) == null) {
                    throw RequiredConfigLackException(require, msg)
                }
            }
        }

        fun getProperties(name: String): Any? {
            val items = name.split('.')
            if (items.size == 1) {
                return properties[items.first()]
            }

            val prefix = items.take(items.size - 1)
            val postfix = items.last()
            var current: Map<String, Any> = properties
            for (item in prefix) {
                if (item in current) {
                    @Suppress("UNCHECKED_CAST")
                    current = current[item] as Map<String, Any>
                } else {
                    return null
                }
            }

            return current[postfix]
        }

        private fun loadAndSelectProfile(
            loader: ClassLoader, cmd: CommandLineArgs
        ) {
            val builder = StringBuilder()
            val (exists, fileConfig) = FileConfig.load(loader)
            if (!exists) {
                builder.append("No default configuration file found, ")
            }

            loadConfig(cmd.toMap())
            loadConfig(fileConfig, false)

            val prefix = getProperties(profilesFilePrefix)?.toString() ?: "config"
            val profile = getProperties(profilesActive)?.toString() ?: ""

            if (profile.isNotEmpty()) {
                val filename = "${prefix}-${profile}"
                builder.append("The flowing profiles are active: $filename")
                log.info(builder)
                val (e, f) = FileConfig.load(loader, filename)
                if (!e) {
                    log.warn("Profile $filename not found")
                    return
                } else {
                    loadConfig(f, true)
                }
            } else {
                builder.append("No active profile set, enabling default setting")
                log.info(builder)
            }
        }

        private fun loadConfig(config: Map<String, Any>, override: Boolean = true) {
            fun dfs(
                source: Map<String, Any>, path: Queue<String>,
                receiver: MutableMap<String, Any>
            ) {
                for ((k, v) in source) {
                    if (v is HashMap<*, *>) {
                        path.add(k)
                        @Suppress("UNCHECKED_CAST")
                        dfs(v as HashMap<String, Any>, path, receiver)
                        path.poll()
                    } else {
                        if (path.isEmpty()) {
                            receiver[k] = v
                        } else {
                            val prefix = path.joinToString(".")
                            val fullPath = "${prefix}.$k"
                            receiver[fullPath] = v
                        }
                    }
                }
            }

            val properties = HashMap<String, Any>()
            dfs(config, LinkedList(), properties)
            properties.forEach { (k, v) ->
                placeConfigItem(k, v, override)
            }
        }

        private fun placeConfigItem(
            key: String, value: Any, override: Boolean = true
        ) {
            fun put(map: MutableMap<String, Any>, k: String) {
                if (!override && k in map)
                    return
                map[k] = value
            }

            if (key in Store.checkers) {
                val checker = CUtils.notnullGet(Store.checkers, key)
                if (!checker(value)) {
                    log.warn("配置项警告: 配置项'$key'使用值'$value', 未通过检查器, 此配置项被忽略")
                    return
                }
            }

            val items = key.split('.')
            if (items.size == 1) {
                return put(properties, key)
            }

            val prefix = items.take(items.size - 1)
            val postfix = items.last()

            var current: MutableMap<String, Any> = this.properties
            for (item in prefix) {
                if (item in current) {
                    @Suppress("UNCHECKED_CAST")
                    current = current[item] as MutableMap<String, Any>
                } else {
                    val new = HashMap<String, Any>()
                    current[item] = new
                    current = new
                }
            }

            put(current, postfix)
        }

    }

    private object FileConfig {

//        const val defConfigName = "config.toml"

        // https://github.com/mwanji/toml4j
        private val toml = Toml()

        fun load(loader: ClassLoader, filename: String = "config"): Pair<Boolean, MutableMap<String, Any>> {
            val fullFilename = filename(filename)
            val (exists, content) = loadFile(loader, fullFilename)
            if (!exists) return false to mutableMapOf()
            val result = kotlin.runCatching { parseTomlFile(content) }
            if (result.isFailure) {
                log.warn("配置文件解析错误: 配置文件名'$fullFilename'", result.exceptionOrNull())
            }
            return true to result.getOrDefault(mutableMapOf())
        }

        fun filename(prefix: String): String {
            return "${prefix}.toml"
        }

        private fun loadFile(loader: ClassLoader, filename: String): Pair<Boolean, String> {
            val stream = loader.getResourceAsStream(filename) ?: return false to Constant.EMPTY_STR
            val buff = stream.bufferedReader()
            val text = buff.readText()
            buff.close()
            return true to text
        }

        private fun parseTomlFile(text: String): MutableMap<String, Any> {
            return toml.read(text).toMap()
        }

    }

    private class TypeMismatchingException(type: KClass<*>, cause: Throwable?) : RuntimeException(
        "配置异常: 生成目标类型'$type'时遇到异常, 请检查配置文件内容是否正确", cause
    )

}
