package com.arif.randomcheckin.data.model

enum class ThemeMode {
    LIGHT,
    DARK;

    companion object {
        fun fromStoredValue(raw: String?): ThemeMode {
            return raw?.let { value -> values().firstOrNull { it.name == value } } ?: LIGHT
        }
    }
}
