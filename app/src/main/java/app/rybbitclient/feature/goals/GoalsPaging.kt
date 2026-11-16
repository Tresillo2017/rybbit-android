package app.rybbitclient.feature.goals

import app.rybbitclient.core.model.GoalsResponse
import app.rybbitclient.core.model.PageMeta

/**
 * Whether another page exists after [page]. Uses `meta.totalPages` when the server sends it, otherwise
 * derives the page count from `meta.total` (which arrives as a string and is decoded by core). Without
 * any metadata, a full page means there may be more.
 */
fun hasMorePages(meta: PageMeta?, page: Int, pageSize: Int, received: Int): Boolean {
    val totalPages = meta?.totalPages ?: meta?.total?.let { total -> ceilDiv(total, pageSize) }
    return if (totalPages != null) page < totalPages else received >= pageSize
}

/** Appends one fetched [response] for [page]. Goals already shown are dropped so a shifted page cannot duplicate rows. */
fun GoalsUiState.Content.withPage(response: GoalsResponse, page: Int, pageSize: Int): GoalsUiState.Content =
    copy(
        goals = (goals + response.data).distinctBy { it.goalId },
        page = page,
        hasMore = hasMorePages(response.meta, page, pageSize, response.data.size),
        isLoadingMore = false,
        loadMoreError = null,
    )

private fun ceilDiv(total: Int, divisor: Int): Int = if (total <= 0) 0 else (total + divisor - 1) / divisor
