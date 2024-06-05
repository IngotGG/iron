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
    fun deserialize(obj: Any, clazz: Class<*>): Any?

    class Kotlinx(private val json: kotlinx.serialization.json.Json) : SerializationAdapter {
        override fun deserialize(obj: Any, clazz: Class<*>): Any {
            val serializer = json.serializersModule.serializerOrNull(clazz)
                ?: error("No serializer found for type: ${clazz.simpleName}")
            return json.decodeFromString(serializer, obj.toString())
        }
    }

    class Gson(private val gson: com.google.gson.Gson) : SerializationAdapter {
        override fun deserialize(obj: Any, clazz: Class<*>): Any {
            return gson.fromJson(obj.toString(), clazz)
        }
    }
}