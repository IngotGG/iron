package gg.ingot.iron.serialization

import kotlinx.serialization.serializerOrNull

/**
 * Adapter for deserializing SQL Json to Kotlin objects.
 * @author DebitCardz
 * @since 1.3
 */
interface SerializationAdapter {
    /**
     * Deserialize the given object into the given class.
     * @param obj The object to deserialize.
     * @param clazz The class to deserialize the object into.
     * @return The deserialized object.
     */
    fun deserialize(obj: Any, clazz: Class<*>): Any

    /**
     * Serialize the given object into a string.
     * @param obj The object to serialize.
     * @param clazz The class of the object.
     * @return The serialized object.
     */
    fun serialize(obj: Any, clazz: Class<*>): String

    class Kotlinx(private val json: kotlinx.serialization.json.Json) : SerializationAdapter {
        override fun deserialize(obj: Any, clazz: Class<*>): Any {
            val serializer = json.serializersModule.serializerOrNull(clazz)
                ?: error("No serializer found for type: ${clazz.simpleName}")
            return json.decodeFromString(serializer, obj.toString())
        }

        override fun serialize(obj: Any, clazz: Class<*>): String {
            val serializer = json.serializersModule.serializerOrNull(clazz)
                ?: error("No serializer found for type: ${clazz.simpleName}")

            return json.encodeToString(serializer, obj)
        }
    }

    class Gson(private val gson: com.google.gson.Gson) : SerializationAdapter {
        override fun deserialize(obj: Any, clazz: Class<*>): Any {
            return gson.fromJson(obj.toString(), clazz)
        }

        override fun serialize(obj: Any, clazz: Class<*>): String {
            return gson.toJson(obj, clazz)
        }
    }
}