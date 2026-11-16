package app.rybbitclient.core.model

import app.rybbitclient.core.network.LenientLongSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An organization the credential can see. `GET /api/organizations` returns a bare array of these. */
@Serializable
data class Organization(
    val id: String,
    val name: String? = null,
    val slug: String? = null,
    val logo: String? = null,
    val createdAt: String? = null,
    val role: String? = null,
    val sites: List<Site> = emptyList(),
)

/** A site. `id` is a hex id on some endpoints and the numeric id as text on others; [analyticsId] is the URL id. */
@Serializable
data class Site(
    val id: String,
    @Serializable(with = LenientLongSerializer::class)
    val siteId: Long? = null,
    val name: String? = null,
    val type: String? = null,
    val domain: String? = null,
    val organizationId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val createdBy: String? = null,
    @SerialName("public") val isPublic: Boolean = false,
    val blockBots: Boolean = false,
    val isOwner: Boolean? = null,
    val trackErrors: Boolean? = null,
    val webVitals: Boolean? = null,
    val sessionReplay: Boolean? = null,
) {
    /** The id used in `/api/sites/{site}/...` URLs. */
    val analyticsId: String
        get() = siteId?.toString() ?: id
}

/** `GET /api/organizations/{id}/sites`: the organization plus its sites. */
@Serializable
data class OrganizationSites(
    val organization: Organization? = null,
    val sites: List<Site> = emptyList(),
)
