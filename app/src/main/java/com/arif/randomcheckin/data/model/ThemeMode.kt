package com.arif.randomcheckin.data.model

/**
 * App theme has two explicit modes; keeping the enum small avoids drift between user preference,
 * persisted storage, and the UI toggle when future palettes are introduced.
 */
enum class ThemeMode {
    LIGHT,
    DARK;

    companion object {
        /**
         * Stored values remain strings in preferences; invalid or legacy values fall back to LIGHT so
         * the app never crashes on unknown input after upgrades.
         */
        fun fromStoredValue(raw: String?): ThemeMode = raw?.let { storedValue ->
            runCatching { valueOf(storedValue) }.getOrNull()
        } ?: LIGHT
    }
}
