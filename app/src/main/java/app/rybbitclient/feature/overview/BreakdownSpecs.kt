package app.rybbitclient.feature.overview

/** One tab of a breakdown card. [parameter] is the metric query parameter, [label] the tab title. */
data class BreakdownTab(val parameter: String, val label: String)

/** A breakdown card on the Overview: a stable [id] for state, a title, and its tabs in order. */
data class BreakdownCardSpec(val id: String, val title: String, val tabs: List<BreakdownTab>)

/** The five breakdown cards, in display order. */
val BREAKDOWN_CARDS: List<BreakdownCardSpec> = listOf(
    BreakdownCardSpec(
        id = "pages",
        title = "Pages",
        tabs = listOf(
            BreakdownTab("pathname", "Pages"),
            BreakdownTab("page_title", "Titles"),
            BreakdownTab("entry_page", "Entry"),
            BreakdownTab("exit_page", "Exit"),
        ),
    ),
    BreakdownCardSpec(
        id = "sources",
        title = "Sources",
        tabs = listOf(
            BreakdownTab("referrer", "Referrer"),
            BreakdownTab("channel", "Channel"),
            BreakdownTab("utm_source", "UTM source"),
            BreakdownTab("utm_medium", "UTM medium"),
            BreakdownTab("utm_campaign", "UTM campaign"),
        ),
    ),
    BreakdownCardSpec(
        id = "locations",
        title = "Locations",
        tabs = listOf(
            BreakdownTab("country", "Country"),
            BreakdownTab("region", "Region"),
            BreakdownTab("city", "City"),
        ),
    ),
    BreakdownCardSpec(
        id = "devices",
        title = "Devices",
        tabs = listOf(
            BreakdownTab("browser", "Browser"),
            BreakdownTab("operating_system", "OS"),
            BreakdownTab("device_type", "Device"),
        ),
    ),
    BreakdownCardSpec(
        id = "language",
        title = "Language",
        tabs = listOf(BreakdownTab("language", "Language")),
    ),
)

/** The parameter of the tab at [index], falling back to the first tab for an out-of-range index. */
fun BreakdownCardSpec.parameterAt(index: Int): String = tabs.getOrElse(index) { tabs.first() }.parameter
