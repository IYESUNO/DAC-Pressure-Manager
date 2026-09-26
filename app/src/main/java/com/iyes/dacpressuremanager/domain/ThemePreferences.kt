package com.iyes.dacpressuremanager.domain

data class ThemePreferences(
    val diamond: ThemePalette = ThemePalette.DEFAULT,
    val ruby: ThemePalette = ThemePalette.DEFAULT,
    val appearance: ThemeAppearance = ThemeAppearance.SYSTEM,
) {
    fun paletteFor(mode: PressureMode): ThemePalette =
        if (mode == PressureMode.DIAMOND) diamond else ruby
}

enum class ThemeAppearance {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun useDarkTheme(systemDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStorage(value: String?): ThemeAppearance =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

enum class ThemePalette {
    DEFAULT,
    GRAY,
    MAUVE,
    SLATE,
    SAGE,
    OLIVE,
    SAND,
    TOMATO,
    RED,
    RUBY,
    CRIMSON,
    PINK,
    PLUM,
    PURPLE,
    VIOLET,
    IRIS,
    INDIGO,
    BLUE,
    CYAN,
    TEAL,
    JADE,
    GREEN,
    GRASS,
    LIME,
    MINT,
    SKY,
    GOLD,
    BRONZE,
    BROWN,
    ORANGE,
    AMBER,
    YELLOW,
    ;

    companion object {
        // Keep stored names and neutral scales compatible, but do not offer six
        // nearly identical neutral palettes as accent themes.
        val selectable: List<ThemePalette> = entries.filterNot {
            it in setOf(GRAY, MAUVE, SLATE, SAGE, OLIVE, SAND)
        }

        fun fromStorage(value: String?): ThemePalette =
            selectable.firstOrNull { it.name == value } ?: DEFAULT
    }
}
