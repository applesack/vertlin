package xyz.scootaloo.vertlin.boot.config

import com.moandjiezana.toml.Toml
import xyz.scootaloo.vertlin.boot.command.CommandLineArgs
import xyz.scootaloo.vertlin.boot.config.PropertySources.RangeChecker
import xyz.scootaloo.vertlin.boot.core.X
import xyz.scootaloo.vertlin.boot.core.ifNotNull
import xyz.scootaloo.vertlin.boot.exception.RequiredConfigLackException
import xyz.scootaloo.vertlin.boot.internal.Extensions
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * @author flutterdash@qq.com
 * @since 2022/8/15 下午10:03
 */
internal object PropertySources {

    private val log = X.getLogger(this::class)
    private val properties = HashMap<String, Any>()

    private const val CONFIG_PREFIX = "profiles.prefix"
    private const val PROFILES_ACTIVE = "profiles.active"

    private const val DEF_CONFIG_FILE = "config.toml"

    fun getProperty(key: String): Any? {
        val items = key.split('.')
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

    /**
     * 读取配置顺序
     * 1. 当前项目config.toml
     * 2. 外部(jar外)配置config.toml
     * 3. 命令行
     * 4. 所有jar中提供的默认配置default.toml
     *
     * 从已存在的信息中检查是否有profile, 如果有, 再次按照新的文件名读取1, 2
     */
    fun loadConfigs(providers: Collection<ConfigProvider>, cmd: CommandLineArgs) {
        for (provider in providers) {
            provider.register(Store)
        }

        val builder = StringBuilder()

        val exists = readCurrentProjectConfigs(DEF_CONFIG_FILE, false)
        readOuterConfig(DEF_CONFIG_FILE)
        loadConfig(cmd.toMap(), true)

        if (!exists) {
            builder.append("No default configuration file found, ")
        }

        loadConfigFromFiles(Extensions.loadDefaultConfigs(), false)

        val profileName = selectProfile(builder) ?: return
        val innerExists = readCurrentProjectConfigs(profileName, true)
        val outerExists = readOuterConfig(profileName)
        if (!innerExists && !outerExists) {
            log.warn("Profile $profileName not found")
        }

        for (required in Store.requires) {
            if (getProperty(required) == null) {
                throw RequiredConfigLackException(required, "")
            }
        }
    }

    private fun selectProfile(builder: StringBuilder): String? {
        val prefix = getProperty(CONFIG_PREFIX)?.toString() ?: "config"
        val active = getProperty(PROFILES_ACTIVE)?.toString() ?: ""
        if (active.isEmpty()) {
            builder.append("No active profile set, enabling default setting")
            log.info(builder)
            return null
        }

        val profile = if (prefix.isEmpty()) "${active}.toml" else "${prefix}-${active}.toml"
        builder.append("The flowing profiles are active: $profile")
        log.info(builder)
        return profile
    }

    private fun readCurrentProjectConfigs(filename: String, overwrite: Boolean): Boolean {
        val loader = loader()
        val stream = loader.getResourceAsStream(filename) ?: return false
        stream.use { input ->
            input.bufferedReader().use { buffered ->
                val toml = buffered.readText()
                loadConfigFromFiles(listOf(toml), overwrite)
            }
        }
        return true
    }

    private fun readOuterConfig(filename: String, overwrite: Boolean = true): Boolean {
        val file = File(filename)
        if (!file.exists()) {
            return false
        }

        val result = runCatching { file.readText() }
        if (result.isSuccess) {
            loadConfigFromFiles(listOf(result.getOrThrow()), overwrite)
        }
        return true
    }

    private fun loadConfigFromFiles(files: Collection<String>, overwrite: Boolean) {
        for (content in files) {
            val result = runCatching { FileConfigParser.parseTomlFileConfig(content) }
            if (result.isFailure) {
                val msg = "配置错误: 解析META-INF/default.toml时发生错误"
                log.error(msg, result.exceptionOrNull())
                continue
            }

            loadConfig(result.getOrThrow(), overwrite)
        }
    }

    private fun loadConfig(
        map: Map<String, Any>, overwrite: Boolean,
        path: Deque<String> = LinkedList()
    ) {
        for ((key, value) in map) {
            val confKey = if (path.isEmpty()) {
                key
            } else {
                val prefix = path.joinToString(".")
                "${prefix}.$key"
            }
            if (confKey in Store.checkers) {
                checkAndRegister(confKey, value, overwrite)
            } else if (value is Map<*, *>) {
                path.addLast(key)
                @Suppress("UNCHECKED_CAST")
                loadConfig(value as Map<String, Any>, overwrite, path)
                path.removeLast()
            } else {
                register(confKey, value)
            }
        }
    }

    private fun checkAndRegister(key: String, value: Any, overwrite: Boolean) {
        if (!overwrite && exists(key)) {
            return
        }

        var converted = value
        if (key in Store.checkers) {
            val checker = Store.checkers[key]!!
            val typeResult = runCatching { simpleTransfer(value, checker.type) }
            if (typeResult.isFailure) {
                val msg = typeError(key, value, checker.type)
                log.warn(msg)
                return
            }

            converted = typeResult.getOrThrow()
            if (converted::class != checker.type && !converted::class.isSubclassOf(checker.type)) {
                val msg = typeError(key, value, checker.type)
                log.warn(msg)
                return
            }

            checker.checker.ifNotNull {
                if (!it.check(converted)) {
                    val tips = checker.tips ?: "类型检查未通过"
                    val msg = "配置错误: 配置项'$key', $tips, 该项已被忽略"
                    log.warn(msg)
                    return
                }
            }

        }

        register(key, converted)
    }

    private fun register(key: String, value: Any) {
        val items = key.split('.')
        if (items.size == 1) {
            properties[items.first()] = value
        }

        val prefix = items.take(items.size - 1)
        var current: MutableMap<String, Any> = properties
        for (item in prefix) {
            if (item in current) {
                @Suppress("UNCHECKED_CAST")
                current = current[item] as MutableMap<String, Any>
            } else {
                current[item] = HashMap<String, Any>()
                @Suppress("UNCHECKED_CAST")
                current = current[item] as MutableMap<String, Any>
            }
        }

        current[items.last()] = value
    }

    private fun exists(key: String): Boolean {
        val items = key.split('.')
        var current: MutableMap<String, Any> = properties
        for (item in items.take(items.size - 1)) {
            if (item in current) {
                @Suppress("UNCHECKED_CAST")
                current = current[item] as MutableMap<String, Any>
            } else {
                return false
            }
        }
        return items.last() in current
    }

    private fun simpleTransfer(value: Any, type: KClass<*>): Any {
        return when (type) {
            Boolean::class -> value.toString().toBoolean()
            Short::class -> (value as Number).toShort()
            Int::class -> (value as Number).toInt()
            Float::class -> (value as Number).toFloat()
            Double::class -> (value as Number).toDouble()
            Long::class -> (value as Number).toLong()
            String::class -> value.toString()
            else -> value
        }
    }

    private fun loader(): ClassLoader {
        return Thread.currentThread().contextClassLoader
    }

    private fun typeError(key: String, value: Any, type: KClass<*>): String {
        return "配置错误: 配置项'$key'格式错误, 类型应该为${type}," +
                " 实际值为$value, 该项已被忽略"
    }

    private object FileConfigParser {

        private val toml = Toml()

        fun parseTomlFileConfig(tomlFile: String): Map<String, Any> {
            return toml.read(tomlFile).toMap()
        }

    }

    internal object Store : ConfigCheckerEditor {

        val checkers = HashMap<String, CheckerWrapper>()
        val requires = HashSet<String>()

        override fun <T : Any> key(
            k: String, type: KClass<T>, init: ConfigCheckerEditor.Checker<T>.() -> Unit
        ) {
            val checker = Checker<T>()
            checker.init()
            if (checker.required) {
                requires.add(k)
            }
            val tips = checker.tips
            @Suppress("UNCHECKED_CAST")
            checkers[k] = CheckerWrapper(tips, type, checker.checker as RangeChecker<Any>?)
        }

    }

    private class Checker<T>(
        var required: Boolean = false,
        var checker: RangeChecker<T>? = null,
        var tips: String? = null
    ) : ConfigCheckerEditor.Checker<T> {

        override fun range(verify: (T) -> Boolean) {
            this.checker = RangeChecker { verify(it) }
        }

        override fun rangeTips(tips: String) {
            this.tips = tips
        }

        override fun required() {
            this.required = true
        }

    }

    internal fun interface RangeChecker<T> {

        fun check(value: T): Boolean

    }

    internal class CheckerWrapper(
        val tips: String?,
        val type: KClass<*>,
        val checker: RangeChecker<Any>?
    )

}
