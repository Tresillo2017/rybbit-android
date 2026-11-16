package app.rybbitclient.core.query

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** The 14 comparison types accepted in a filter object. [apiValue] is the wire value. */
@Serializable
enum class FilterType(val apiValue: String) {
    @SerialName("equals") Equals("equals"),
    @SerialName("not_equals") NotEquals("not_equals"),
    @SerialName("contains") Contains("contains"),
    @SerialName("not_contains") NotContains("not_contains"),
    @SerialName("starts_with") StartsWith("starts_with"),
    @SerialName("ends_with") EndsWith("ends_with"),
    @SerialName("regex") Regex("regex"),
    @SerialName("not_regex") NotRegex("not_regex"),
    @SerialName("is_null") IsNull("is_null"),
    @SerialName("is_not_null") IsNotNull("is_not_null"),
    @SerialName("greater_than") GreaterThan("greater_than"),
    @SerialName("less_than") LessThan("less_than"),
    @SerialName("greater_than_or_equal") GreaterThanOrEqual("greater_than_or_equal"),
    @SerialName("less_than_or_equal") LessThanOrEqual("less_than_or_equal"),
}

/**
 * One filter, e.g. `browser equals [Firefox]`. Serializes to exactly the shape the server expects:
 * `{"parameter":"browser","type":"equals","value":["Firefox"]}`.
 */
@Serializable
data class Filter(
    val parameter: String,
    val type: FilterType,
    @SerialName("value") val values: List<String>,
)

/** Encodes filters into the JSON array sent in the `filters` query parameter. */
object FilterEncoder {

    private val json = Json { encodeDefaults = true }
    private val serializer: KSerializer<List<Filter>> = ListSerializer(Filter.serializer())

    fun encode(filters: List<Filter>): String = json.encodeToString(serializer, filters)
}
