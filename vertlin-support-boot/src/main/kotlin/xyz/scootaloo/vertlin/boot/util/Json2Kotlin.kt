package xyz.scootaloo.vertlin.boot.util

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.json.impl.JsonUtil
import io.vertx.kotlin.core.json.get
import xyz.scootaloo.vertlin.boot.core.LazyValue
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

/**
 * @author flutterdash@qq.com
 * @since 2022/7/20 下午11:58
 */
object Json2Kotlin {

    @Volatile private var hasInit = false
    private val basicTypeJsonConverters = HashMap<String, BasicConverter>()
    private val collectionFactories = HashMap<String, CollectionFactory>()
    private val mapperFactories = HashMap<String, MapperFactory>()
    private val arrayTypeQualifiedName = TypeUtils.solveQualifiedName(Array::class)

    internal fun initialize() {
        if (hasInit)
            return

        fun register(type: KClass<*>, converter: BasicConverter) {
            basicTypeJsonConverters[TypeUtils.solveQualifiedName(type)] = converter
        }

        fun registerFactory(type: KClass<*>, factory: CollectionFactory) {
            collectionFactories[TypeUtils.solveQualifiedName(type)] = factory
        }

        fun registerMapper(type: KClass<*>, factory: MapperFactory) {
            mapperFactories[TypeUtils.solveQualifiedName(type)] = factory
        }

        register(Boolean::class) { obj -> obj as Boolean }
        register(Byte::class) { obj -> (obj as Number).toByte() }
        register(Short::class) { obj -> (obj as Number).toShort() }
        register(Int::class) { obj -> (obj as Number).toInt() }
        register(Float::class) { obj -> (obj as Number).toFloat() }
        register(Double::class) { obj -> (obj as Number).toDouble() }
        register(Long::class) { obj -> (obj as Number).toLong() }
        register(String::class) { obj ->
            when (obj) {
                is Instant -> DateTimeFormatter.ISO_INSTANT.format(obj)
                is ByteArray -> JsonUtil.BASE64_ENCODER.encodeToString(obj)
                is Buffer -> JsonUtil.BASE64_ENCODER.encodeToString(obj.bytes)
                is Enum<*> -> obj.name
                else -> obj.toString()
            }
        }

        registerFactory(List::class) { ArrayList(it) }
        registerFactory(LinkedList::class) { LinkedList() }
        registerFactory(ArrayList::class) { ArrayList(it) }
        registerFactory(Queue::class) { LinkedList() }
        registerFactory(Deque::class) { LinkedList() }
        registerFactory(Stack::class) { Stack() }
        registerFactory(Set::class) { HashSet(16) }
        registerFactory(HashSet::class) { HashSet(16) }
        registerFactory(LinkedHashSet::class) { LinkedHashSet(16) }

        registerMapper(Map::class) { HashMap() }
        registerMapper(HashMap::class) { HashMap() }
        registerMapper(LinkedHashMap::class) { LinkedHashMap() }
        registerMapper(TreeMap::class) { TreeMap() }
        registerMapper(MutableMap::class) { HashMap() }

        hasInit = true
    }

    fun qualifiedAddressByMethod(prefix: String, func: KFunction<*>): String {
        return "$prefix:${func.name}"
    }

    fun serializeArguments(args: List<Any>): JsonArray {
        return JsonArray(args)
    }

    fun deserializeArguments(
        array: JsonArray, method: Method, count: Int = method.parameterCount
    ): Array<Any?> {
        val paramTypes = method.parameterTypes
        val lazyGenericTypes = LazyValue { method.genericParameterTypes }
        return Array(count) build@{ idx ->
            val value = array.getValue(idx) ?: return@build null
            val ktType = paramTypes[idx].kotlin
            deserialize(ktType, value, lazyGenericTypes, idx, method)
        }
    }

    fun serializeReturnValue(value: Any?): Any? {
        val notnull = value ?: return null
        val ktType = notnull::class
        val typeQName = TypeUtils.solveQualifiedName(ktType)
        if (typeQName in basicTypeJsonConverters) {
            return notnull
        }
        return JsonObject.mapFrom(value)
    }

    fun deserializeReturnValue(value: Any?, method: Method): Any? {
        val notnull = value ?: return null
        val ktType = method.returnType.kotlin
        val lazyGeneric = LazyValue { arrayOf(method.genericReturnType) }
        return deserialize(ktType, notnull, lazyGeneric, 0, method)
    }

    // --------------------------------------------------------------
    //
    //                        反序列化实现
    //
    // --------------------------------------------------------------

    private fun deserialize(
        type: KClass<*>, value: Any,
        generics: LazyValue<Array<Type>>, idx: Int, source: Any
    ): Any {
        val typeQName = TypeUtils.solveQualifiedName(type)
        if (typeQName in basicTypeJsonConverters) {
            return deserializeBasicType(value, typeQName)
        }

        if (typeQName == arrayTypeQualifiedName) {
            return deserializeArray(value, type, typeQName)
        }

        if (typeQName in collectionFactories) {
            return deserializeCollection(value, typeQName, generics.value, idx, source)
        }

        if (typeQName in mapperFactories) {
            return deserializeMapper(value, typeQName, generics.value, idx, source)
        }

        return deserializeObject(value, type)
    }

    private fun deserializeBasicType(value: Any, typeQName: String): Any {
        val converter = basicTypeJsonConverters[typeQName]!!
        return converter.convert(value)
    }

    private fun deserializeArray(any: Any, type: KClass<*>, typeQName: String): Any {
        val array = any as JsonArray
        val componentType = type.java.componentType
        val isBasicType = typeQName in basicTypeJsonConverters
        return Array(array.size()) build@{ idx ->
            val value = array.getValue(idx) ?: return@build null
            if (isBasicType) {
                return@build deserializeBasicType(value, typeQName)
            }
            deserializeObject(array[idx], componentType.kotlin)
        }
    }

    private fun deserializeCollection(
        any: Any, typeQName: String, generics: Array<Type>, idx: Int, source: Any
    ): Any {
        val array = any as JsonArray
        val collection = collectionFactories[typeQName]!!.create(array.size())
        val componentType = TypeUtils.solveParamGenericType(source, generics, idx)[0]
        val compQName = TypeUtils.solveQualifiedName(componentType)
        val isBasic = compQName in basicTypeJsonConverters
        for (element in array) {
            if (isBasic) {
                collection.add(deserializeBasicType(element, compQName))
            } else {
                collection.add(deserializeObject(element, componentType))
            }
        }
        return collection
    }

    private fun deserializeMapper(
        any: Any, typeQName: String, generics: Array<Type>, idx: Int, source: Any
    ): Any {
        val factory = mapperFactories[typeQName]!!
        val genericTypes = TypeUtils.solveParamGenericType(source, generics, idx)
        val keyType = genericTypes[0]
        val valueType = genericTypes[1]
        val json = any as JsonObject
        return factory.build(json, keyType, valueType)
    }

    private fun deserializeObject(any: Any, type: KClass<*>): Any {
        val json = any as JsonObject
        val primaryConstructor = type.primaryConstructor
        return if (primaryConstructor!!.parameters.isEmpty()) {
            json.mapTo(type.java)
        } else {
            constructObject(json, primaryConstructor)
        }
    }

    private fun constructObject(json: JsonObject, kConstructor: KFunction<*>): Any {
        val paramNames = kConstructor.parameters.map { it.name!! }
        val jConstructor = kConstructor.javaConstructor!!
        jConstructor.isAccessible = true

        val paramTypes = jConstructor.parameterTypes
        val lazyParamGenerics = LazyValue { jConstructor.genericParameterTypes }
        val arguments = Array(jConstructor.parameterCount) build@{ idx ->
            val value = json.getValue(paramNames[idx]) ?: return@build null
            val ktType = paramTypes[idx].kotlin
            deserialize(ktType, value, lazyParamGenerics, idx, jConstructor)
        }

        return jConstructor.newInstance(*arguments) as Any
    }

    fun interface BasicConverter {

        fun convert(any: Any): Any

    }

    fun interface CollectionFactory {

        fun create(size: Int): MutableCollection<Any>

    }

    fun interface MapperFactory {

        fun create(): MutableMap<Any, Any>

        fun build(
            json: JsonObject, keyType: KClass<*>, valueType: KClass<*>
        ): Map<Any, Any> {
            return create().apply {
                val keyGen: java.util.function.Function<Any, Any> =
                    when (
                        val keyTypeQName = TypeUtils.solveQualifiedName(keyType)
                    ) {
                        in basicTypeJsonConverters -> {
                            java.util.function.Function { k ->
                                basicTypeJsonConverters[keyTypeQName]!!.convert(k)
                            }
                        }
                        else -> {
                            java.util.function.Function { k ->
                                deserializeObject(k, valueType)
                            }
                        }
                    }

                val valueGen: java.util.function.Function<Any, Any> =
                    when (
                        val valueTypeQName = TypeUtils.solveQualifiedName(valueType)
                    ) {
                        in basicTypeJsonConverters -> {
                            java.util.function.Function { v ->
                                basicTypeJsonConverters[valueTypeQName]!!.convert(v)
                            }
                        }
                        arrayTypeQualifiedName -> {
                            java.util.function.Function { v ->
                                deserializeArray(v, valueType, valueTypeQName)
                            }
                        }
                        else -> {
                            java.util.function.Function { v ->
                                deserializeObject(v, valueType)
                            }
                        }
                    }

                for ((key, value) in json) {
                    this[keyGen.apply(key)] = valueGen.apply(value)
                }
            }
        }

    }

}
