package fr.mesactus.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", null)!!) }.getOrDefault(ThemeMode.SYSTEM),
        hiddenSections = prefs.getStringSet("hiddenSections", emptySet())!!
            .mapNotNull { runCatching { Section.valueOf(it) }.getOrNull() }.toSet(),
    )

    fun save(s: Settings) {
        prefs.edit()
            .putString("theme", s.theme.name)
            .putStringSet("hiddenSections", s.hiddenSections.map { it.name }.toSet())
            .apply()
    }
}
