package app.rybbitclient.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Reads a JSON primitive as text, accepting both numbers and strings. */
private fun Decoder.primitiveContent(): JsonPrimitive {
    val element = (this as? JsonDecoder)?.decodeJsonElement()
        ?: throw SerializationException("Lenient serializers require JSON input")
    return element as? JsonPrimitive ?: throw SerializationException("Expected a JSON primitive but got $element")
}

/** Accepts `3`, `"3"` or `3.0` for an Int field. Non-numeric values are rejected, never guessed. */
object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val primitive = decoder.primitiveContent()
        return primitive.longOrNull?.toInt()
            ?: primitive.doubleOrNull?.toInt()
            ?: throw SerializationException("Expected an integer but got ${primitive.content}")
    }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Int) = encoder.encodeInt(value)
}

/** Accepts a JSON number or numeric string for a Long field. */
object LenientLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val primitive = decoder.primitiveContent()
        return primitive.longOrNull
            ?: primitive.doubleOrNull?.toLong()
            ?: throw SerializationException("Expected a long but got ${primitive.content}")
    }

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

/** Accepts a JSON number or numeric string for a Double field. */
object LenientDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val primitive = decoder.primitiveContent()
        return primitive.doubleOrNull
            ?: throw SerializationException("Expected a number but got ${primitive.content}")
    }

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}

/**
 * Accepts a string or a number and keeps its text form. Used for version fields such as
 * `browser_version` (a number in one fixture) and `operating_system_version` (a string in another).
 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String = decoder.primitiveContent().content

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}
