package app.rybbitclient.feature.sites

import java.util.concurrent.atomic.AtomicBoolean

/**
 * One-shot permission to auto-open the remembered site. Claimed at most once per gate, so returning to the
 * site list after leaving a site does not bounce the user straight back in.
 */
class AutoOpenGate {

    private val claimed = AtomicBoolean(false)

    /** True only for the first caller. */
    fun tryClaim(): Boolean = claimed.compareAndSet(false, true)
}

/** Process-wide gate: the remembered site opens once per app launch. */
val ProcessAutoOpenGate = AutoOpenGate()
